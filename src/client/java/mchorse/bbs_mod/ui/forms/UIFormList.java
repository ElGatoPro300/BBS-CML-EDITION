package mchorse.bbs_mod.ui.forms;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.FormCategories;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.categories.FormCategory;
import mchorse.bbs_mod.forms.categories.UserFormCategory;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.forms.ParticleForm;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.categories.UIFormCategory;
import mchorse.bbs_mod.ui.forms.categories.UIRecentFormCategory;
import mchorse.bbs_mod.ui.forms.overlays.UIFavoriteCategoryOverlayPanel;
import mchorse.bbs_mod.ui.forms.overlays.UIRemoveFavoriteCategoryOverlayPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.navigation.UIControlBar;
import mchorse.bbs_mod.ui.framework.elements.navigation.UIIconTabButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIMessageOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.EventPropagation;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.client.render.DiffuseLighting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class UIFormList extends UIElement
{
    private static final String FAVORITES_CATEGORY_ID = "favorites";
    private static final int FAVORITES_TOP_BAR_HEIGHT = 20;
    private static final int FAVORITES_TOP_BAR_BOTTOM_SPACING = 10;
    private static final int TAB_WIDTH_ALL = 88;
    private static final int TAB_WIDTH_FAVORITES = 108;
    private static final int TAB_WIDTH_CUSTOM = 122;
    private static final int TAB_WIDTH_ADD = 24;
    private static final int ACTIONS_BAR_HEIGHT = 20;
    private static final int SELECTED_INFO_WIDTH = 200;
    private static final int CATEGORY_CARD_WIDTH = 180;
    private static final int CATEGORY_CARD_HEIGHT = 140;
    private static final int CATEGORY_CARD_GAP = 10;
    private static final int CATEGORY_PREVIEW_PADDING = 8;
    private static final int CATEGORY_PREVIEW_GAP = 4;
    private static final int CATEGORY_PREVIEW_COLUMNS = 3;
    private static final int CATEGORY_PREVIEW_ROWS = 3;
    private static final int POPUP_CELL_WIDTH = 80;
    private static final int POPUP_CELL_HEIGHT = 100;
    private static final int MAX_CATEGORY_NAME_LENGTH = 20;
    private static final int MAX_TAB_TITLE_LENGTH = 10;
    private static final long SEARCH_DEBOUNCE_MS = 150L;
    private static final int CATEGORY_VIRTUALIZATION_BUFFER_ROWS = 1;
    private static final int CATEGORY_PREVIEW_TOGGLE_SIZE = 14;
    private static final int CATEGORY_HIDDEN_ICON_SIZE = 50;

    public IUIFormList palette;

    public UIScrollView forms;
    public UIScrollView categoryCardsView;

    public UIElement tabsBar;
    public UIElement tabs;
    public UIElement bar;
    public UITextbox search;
    public UIButton allTab;
    public UIButton favoritesTab;
    public UIButton addCategoryTab;
    public UIIcon toggleAllPreviews;
    public UIIcon edit;
    public UIIcon close;

    private UIFormCategory recent;
    private List<UIFormCategory> categories = new ArrayList<>();
    private final Set<String> favoriteModelForms = new HashSet<>();
    private final LinkedHashMap<String, FavoriteCategory> customFavoriteCategories = new LinkedHashMap<>();
    private final Map<String, Set<String>> customCategoryForms = new HashMap<>();
    private final List<UIIconTabButton> customCategoryTabs = new ArrayList<>();
    private final Map<UIIconTabButton, String> customCategoryTabIds = new HashMap<>();
    private String activeFavoriteCategoryId;
    private boolean favoritesUiVisible;
    private String syncedFavoriteCategoriesData = "";
    private final Set<String> syncedFavoriteModels = new HashSet<>();
    private Consumer<String> favoriteCategoryChanged;

    private long lastUpdate;
    private int lastScroll;
    private UIElement categoryPopup;
    private UICategoryCardsGrid categoryCards;
    private final Set<String> hiddenCategoryPreviews = new HashSet<>();
    private String pendingSearchQuery = "";
    private String appliedSearchQuery = "";
    private long pendingSearchDeadline = -1L;

    public UIFormList(IUIFormList palette)
    {
        this.palette = palette;
        this.loadFavoriteDataFromSettings();

        this.forms = UI.scrollView(0, 0);
        this.forms.scroll.cancelScrolling();
        this.categoryCardsView = UI.scrollView(0, 0);
        this.categoryCardsView.scroll.cancelScrolling();
        this.tabsBar = new UIControlBar();
        this.tabs = new UIElement();
        this.categoryCards = new UICategoryCardsGrid();
        this.bar = new UIElement()
        {
            @Override
            public void render(UIContext context)
            {
                context.batcher.getContext().getMatrices().push();
                context.batcher.getContext().getMatrices().translate(0, 0, 200);
                this.area.render(context.batcher, Colors.CONTROL_BAR);
                super.render(context);

                Form selected = UIFormList.this.getSelected();

                if (selected != null)
                {
                    FontRenderer font = context.batcher.getFont();
                    int x = this.area.x + 4;
                    int y = this.area.y;
                    int maxTextWidth = SELECTED_INFO_WIDTH - 10;
                    String display = font.limitToWidth(selected.getDisplayName() == null ? "" : selected.getDisplayName(), maxTextWidth);
                    String valueId = font.limitToWidth(selected.getFormId() == null ? "" : selected.getFormId(), maxTextWidth);

                    context.batcher.textShadow(display, x, y + 1);
                    context.batcher.textShadow(valueId, x, y + 10, Colors.LIGHTEST_GRAY);
                }

                context.batcher.getContext().getMatrices().pop();
            }
        };
        this.search = new UITextbox(100, this::search).placeholder(UIKeys.FORMS_LIST_SEARCH);
        this.allTab = new UIIconTabButton(UIKeys.FORMS_LIST_TAB_ALL, Icons.LIST, (b) -> this.setActiveFavoriteCategory(null));
        this.favoritesTab = new UIIconTabButton(UIKeys.FORMS_LIST_TAB_FAVORITES, Icons.FIVE_STAR, (b) -> this.setActiveFavoriteCategory(FAVORITES_CATEGORY_ID));
        this.addCategoryTab = new UIIconTabButton(IKey.constant(""), Icons.ADD, (b) -> this.openCreateCategoryPanel());
        this.toggleAllPreviews = new UIIcon(() -> this.areAllCategoryPreviewsVisible() ? Icons.VISIBLE : Icons.INVISIBLE, this::toggleAllCategoryPreviews);
        this.toggleAllPreviews.tooltip(IKey.constant("Ocultar/mostrar previews de todas las categorias"), Direction.TOP);
        this.edit = new UIIcon(Icons.EDIT, this::edit);
        this.edit.tooltip(UIKeys.FORMS_LIST_EDIT, Direction.TOP);
        this.close = new UIIcon(Icons.CLOSE, this::close);

        this.forms.relative(this).x(0).y(ACTIONS_BAR_HEIGHT).w(1F).h(1F, -ACTIONS_BAR_HEIGHT);
        this.categoryCardsView.relative(this).x(0).y(ACTIONS_BAR_HEIGHT).w(1F).h(1F, -ACTIONS_BAR_HEIGHT);
        this.categoryCards.relative(this.categoryCardsView).x(0).y(0).w(1F).h(1F);
        this.tabsBar.relative(this).x(0).y(0).w(1F).h(FAVORITES_TOP_BAR_HEIGHT);
        this.tabs.relative(this.tabsBar).x(10).y(0).w(1F, -20).h(FAVORITES_TOP_BAR_HEIGHT).row(0).resize();
        this.bar.relative(this).x(0).y(FAVORITES_TOP_BAR_HEIGHT).w(1F).h(ACTIONS_BAR_HEIGHT);

        this.allTab.w(TAB_WIDTH_ALL).h(FAVORITES_TOP_BAR_HEIGHT);
        this.favoritesTab.w(TAB_WIDTH_FAVORITES).h(FAVORITES_TOP_BAR_HEIGHT);
        this.addCategoryTab.w(TAB_WIDTH_ADD).h(FAVORITES_TOP_BAR_HEIGHT);
        this.addCategoryTab.background(false);
        this.rebuildCategoryTabs();
        this.tabsBar.add(this.tabs);
        this.tabsBar.setVisible(false);
        this.bar.add(this.search, this.toggleAllPreviews, this.edit, this.close);
        this.categoryCardsView.add(this.categoryCards);
        this.forms.setVisible(false);
        this.layoutActionBar();
        this.add(this.forms, this.categoryCardsView, this.bar, this.tabsBar);

        this.applyFavoritesLayout(this.isFavoritesFeatureEnabled());
        this.updateTabs();

        this.search.keys().register(Keys.FORMS_FOCUS, this::focusSearch);

        this.markContainer();
        this.setupForms(BBSModClient.getFormCategories());
    }

    private void focusSearch()
    {
        this.search.clickItself();
    }

    public void setupForms(FormCategories forms)
    {
        this.closeCategoryPopup();
        this.categories.clear();
        this.forms.removeAll();

        for (FormCategory category : forms.getAllCategories())
        {
            UIFormCategory uiCategory = category.createUI(this);

            this.forms.add(uiCategory);
            this.categories.add(uiCategory);

            if (uiCategory instanceof UIRecentFormCategory)
            {
                this.recent = uiCategory;
            }
        }

        this.categories.get(this.categories.size() - 1).marginBottom(40);
        this.hiddenCategoryPreviews.retainAll(this.getAvailableCategoryPreviewKeys());
        this.applySearchNow(this.appliedSearchQuery);
        this.categoryCardsView.scroll.scrollTo(0);
        this.resize();

        this.lastUpdate = forms.getLastUpdate();
    }

    public boolean supportsFavorites()
    {
        return this.isFavoritesFeatureEnabled();
    }

    public boolean isFavoritesOnly()
    {
        return this.isFavoritesFeatureEnabled() && this.activeFavoriteCategoryId != null;
    }

    public boolean isFavoriteForm(Form form)
    {
        return this.getFavoriteMarker(form) != null;
    }

    public boolean hasCustomFavoriteCategories()
    {
        return !this.customFavoriteCategories.isEmpty();
    }

    public FavoriteMarker getFavoriteMarker(Form form)
    {
        String key = this.getFavoriteKey(form);

        if (key == null)
        {
            return null;
        }

        FavoriteCategory customCategory = this.findCustomCategoryForKey(key);

        if (customCategory != null)
        {
            return new FavoriteMarker(customCategory.id, customCategory.name, this.getIconByName(customCategory.icon), (customCategory.color & Colors.RGB) | Colors.A100);
        }

        if (this.favoriteModelForms.contains(key))
        {
            return new FavoriteMarker(FAVORITES_CATEGORY_ID, UIKeys.FORMS_LIST_TAB_FAVORITES.get(), Icons.FIVE_STAR, Colors.YELLOW | Colors.A100);
        }

        return null;
    }

    public boolean addFavoriteForm(Form form)
    {
        if (this.hasCustomFavoriteCategories())
        {
            this.openAddToCategoryPanel(form);

            return false;
        }

        String key = this.getFavoriteKey(form);

        if (key == null)
        {
            return false;
        }

        Set<String> categoryForms = this.getFormsForCategory(this.getCurrentMarkerCategoryId(), true);

        if (categoryForms.add(key))
        {
            this.persistFavoriteData();

            return true;
        }

        return false;
    }

    public boolean addFavoriteFormToCategory(Form form, String categoryId)
    {
        String key = this.getFavoriteKey(form);

        if (key == null || categoryId == null)
        {
            return false;
        }

        if (!FAVORITES_CATEGORY_ID.equals(categoryId) && !this.customFavoriteCategories.containsKey(categoryId))
        {
            return false;
        }

        boolean changed = false;

        for (Set<String> forms : this.customCategoryForms.values())
        {
            changed = forms.remove(key) || changed;
        }

        changed = this.favoriteModelForms.remove(key) || changed;

        if (FAVORITES_CATEGORY_ID.equals(categoryId))
        {
            changed = this.favoriteModelForms.add(key) || changed;
        }
        else
        {
            Set<String> categoryForms = this.getFormsForCategory(categoryId, true);

            if (categoryForms != null)
            {
                changed = categoryForms.add(key) || changed;
            }
        }

        if (changed)
        {
            this.persistFavoriteData();
        }

        return changed;
    }

    public boolean isFormInCustomFavoriteCategory(Form form)
    {
        FavoriteMarker marker = this.getFavoriteMarker(form);

        return marker != null && !FAVORITES_CATEGORY_ID.equals(marker.categoryId);
    }

    public boolean removeFavoriteForm(Form form)
    {
        String key = this.getFavoriteKey(form);

        if (key == null)
        {
            return false;
        }

        Set<String> currentCategoryForms = this.getFormsForCategory(this.getCurrentMarkerCategoryId(), false);

        if (currentCategoryForms != null && currentCategoryForms.remove(key))
        {
            this.persistFavoriteData();

            return true;
        }

        FavoriteCategory customCategory = this.findCustomCategoryForKey(key);

        if (customCategory != null)
        {
            Set<String> customForms = this.customCategoryForms.get(customCategory.id);

            if (customForms != null && customForms.remove(key))
            {
                this.persistFavoriteData();

                return true;
            }
        }

        if (this.favoriteModelForms.remove(key))
        {
            this.persistFavoriteData();

            return true;
        }

        return false;
    }

    public IKey getAddFavoriteContextLabel()
    {
        if (this.hasCustomFavoriteCategories())
        {
            return UIKeys.FORMS_CATEGORIES_CONTEXT_ADD_TO_PICK;
        }

        return this.getAddToCurrentCategoryLabel();
    }

    public IKey getMoveFavoriteContextLabel()
    {
        return UIKeys.FORMS_CATEGORIES_CONTEXT_MOVE_TO_PICK;
    }

    public IKey getRemoveFavoriteContextLabel(Form form)
    {
        FavoriteMarker marker = this.getFavoriteMarker(form);

        if (marker == null || FAVORITES_CATEGORY_ID.equals(marker.categoryId))
        {
            return UIKeys.FORMS_CATEGORIES_CONTEXT_REMOVE_FROM_FAVORITES;
        }

        return UIKeys.FORMS_CATEGORIES_CONTEXT_REMOVE_FROM_CATEGORY.format(marker.name);
    }

    public void openAddToCategoryPanel(Form form)
    {
        if (form == null || this.customFavoriteCategories.isEmpty())
        {
            return;
        }

        List<FavoriteMarker> options = this.getCategoryPickOptions();
        UICategoryPickOverlayPanel panel = new UICategoryPickOverlayPanel(options, (marker) ->
        {
            if (marker != null)
            {
                this.addFavoriteFormToCategory(form, marker.categoryId);
            }
        });

        FavoriteMarker marker = this.getFavoriteMarker(form);

        if (marker != null)
        {
            panel.setCurrent(marker.categoryId);
        }

        UIOverlay.addOverlay(this.getContext(), panel, 240, 0.6F);
    }

    public boolean shouldDisplayForm(Form form)
    {
        if (!this.isFavoritesOnly())
        {
            return true;
        }

        String key = this.getFavoriteKey(form);
        Set<String> categoryForms = this.getFormsForCategory(this.activeFavoriteCategoryId, false);

        return key != null && categoryForms != null && categoryForms.contains(key);
    }

    public IKey getAddToCurrentCategoryLabel()
    {
        if (this.activeFavoriteCategoryId == null || FAVORITES_CATEGORY_ID.equals(this.activeFavoriteCategoryId))
        {
            return UIKeys.FORMS_CATEGORIES_CONTEXT_ADD_TO_FAVORITES;
        }

        FavoriteCategory category = this.customFavoriteCategories.get(this.activeFavoriteCategoryId);
        String title = category == null ? UIKeys.FORMS_LIST_TAB_FAVORITES.get() : category.name;

        return UIKeys.FORMS_CATEGORIES_CONTEXT_ADD_TO_CATEGORY.format(title);
    }

    public IKey getRemoveFromCurrentCategoryLabel()
    {
        if (this.activeFavoriteCategoryId == null || FAVORITES_CATEGORY_ID.equals(this.activeFavoriteCategoryId))
        {
            return UIKeys.FORMS_CATEGORIES_CONTEXT_REMOVE_FROM_FAVORITES;
        }

        FavoriteCategory category = this.customFavoriteCategories.get(this.activeFavoriteCategoryId);
        String title = category == null ? UIKeys.FORMS_LIST_TAB_FAVORITES.get() : category.name;

        return UIKeys.FORMS_CATEGORIES_CONTEXT_REMOVE_FROM_CATEGORY.format(title);
    }

    private void setActiveFavoriteCategory(String categoryId)
    {
        if (!this.isFavoritesFeatureEnabled() || Objects.equals(this.activeFavoriteCategoryId, categoryId))
        {
            return;
        }

        Form selected = this.getSelected();
        String query = this.search.getText();

        this.activeFavoriteCategoryId = categoryId;
        this.notifyFavoriteCategoryChanged();
        this.updateTabs();
        this.setupForms(BBSModClient.getFormCategories());
        this.applySearchNow(query);
        this.restoreSelectedIfPresent(selected);
        this.forms.scroll.scrollTo(0);
        this.forms.resize();
        this.resize();
    }

    private void updateTabs()
    {
        if (!this.isFavoritesFeatureEnabled())
        {
            return;
        }

        this.allTab.color(this.activeFavoriteCategoryId == null ? BBSSettings.primaryColor.get() : 0x2d2d2d);
        this.favoritesTab.color(FAVORITES_CATEGORY_ID.equals(this.activeFavoriteCategoryId) ? Colors.YELLOW : 0x2d2d2d);

        for (UIIconTabButton tab : this.customCategoryTabs)
        {
            String categoryId = this.customCategoryTabIds.get(tab);
            FavoriteCategory category = this.customFavoriteCategories.get(categoryId);
            int tabColor = category == null ? 0x2d2d2d : category.color;

            tab.color(Objects.equals(this.activeFavoriteCategoryId, categoryId) ? tabColor : 0x2d2d2d);
        }

        this.addCategoryTab.color(0x2d2d2d);
    }

    private void rebuildCategoryTabs()
    {
        this.customCategoryTabs.clear();
        this.customCategoryTabIds.clear();
        this.tabs.removeAll();
        this.tabs.add(this.allTab, this.favoritesTab);

        for (FavoriteCategory category : this.customFavoriteCategories.values())
        {
            UIIconTabButton tab = new UIIconTabButton(IKey.constant(this.getCategoryTabTitle(category.name)), this.getIconByName(category.icon), (b) -> this.setActiveFavoriteCategory(category.id));
            tab.w(TAB_WIDTH_CUSTOM).h(FAVORITES_TOP_BAR_HEIGHT);
            tab.removable((b) -> this.openRemoveCustomCategoryPanel(category));
            tab.context((menu) -> menu.action(Icons.EDIT, UIKeys.FORMS_LIST_CATEGORIES_EDIT_ACTION, () -> this.openEditCustomCategoryPanel(category.id)));
            this.customCategoryTabs.add(tab);
            this.customCategoryTabIds.put(tab, category.id);
            this.tabs.add(tab);
        }

        this.tabs.add(this.addCategoryTab);
        this.tabs.resize();
        this.updateTabs();
    }

    private void removeCustomCategory(String categoryId)
    {
        if (categoryId == null || FAVORITES_CATEGORY_ID.equals(categoryId))
        {
            return;
        }

        if (this.customFavoriteCategories.remove(categoryId) == null)
        {
            return;
        }

        this.customCategoryForms.remove(categoryId);

        if (Objects.equals(this.activeFavoriteCategoryId, categoryId))
        {
            this.activeFavoriteCategoryId = FAVORITES_CATEGORY_ID;
        }

        this.persistFavoriteData();
        this.rebuildCategoryTabs();

        Form selected = this.getSelected();
        String query = this.search.getText();
        this.setupForms(BBSModClient.getFormCategories());
        this.applySearchNow(query);
        this.restoreSelectedIfPresent(selected);
    }

    private void openRemoveCustomCategoryPanel(FavoriteCategory category)
    {
        if (category == null)
        {
            return;
        }

        UIRemoveFavoriteCategoryOverlayPanel panel = new UIRemoveFavoriteCategoryOverlayPanel(category.name, (confirm) ->
        {
            if (confirm)
            {
                this.removeCustomCategory(category.id);
            }
        });

        UIOverlay.addOverlay(this.getContext(), panel, 320, 140);
    }

    private void openCreateCategoryPanel()
    {
        UIFavoriteCategoryOverlayPanel panel = new UIFavoriteCategoryOverlayPanel(this::createCustomCategory);

        UIOverlay.addOverlay(this.getContext(), panel, 260, 160);
    }

    private void openEditCustomCategoryPanel(String categoryId)
    {
        FavoriteCategory category = this.customFavoriteCategories.get(categoryId);

        if (category == null)
        {
            return;
        }

        UIFavoriteCategoryOverlayPanel.FavoriteCategoryData data = new UIFavoriteCategoryOverlayPanel.FavoriteCategoryData(category.name, category.icon, category.color);
        UIFavoriteCategoryOverlayPanel panel = new UIFavoriteCategoryOverlayPanel(
            UIKeys.FORMS_LIST_CATEGORIES_EDIT_TITLE,
            UIKeys.GENERAL_EDIT,
            data,
            (updatedData) -> this.editCustomCategory(categoryId, updatedData)
        );

        UIOverlay.addOverlay(this.getContext(), panel, 260, 160);
    }

    private boolean createCustomCategory(UIFavoriteCategoryOverlayPanel.FavoriteCategoryData data)
    {
        String name = data.name == null ? "" : data.name.trim();

        if (name.isEmpty())
        {
            return false;
        }

        if (name.length() > MAX_CATEGORY_NAME_LENGTH)
        {
            name = name.substring(0, MAX_CATEGORY_NAME_LENGTH);
        }

        if (this.hasCategoryName(name))
        {
            UIOverlay.addOverlay(this.getContext(), new UIMessageOverlayPanel(
                UIKeys.GENERAL_WARNING,
                UIKeys.FORMS_LIST_CATEGORIES_CREATE_EXISTS.format(name)
            ), 320, 120);

            return false;
        }

        String id = this.generateUniqueCategoryId(name);
        FavoriteCategory category = new FavoriteCategory(id, name, data.iconName, data.color & Colors.RGB);

        this.customFavoriteCategories.put(id, category);
        this.customCategoryForms.put(id, new HashSet<>());
        this.activeFavoriteCategoryId = id;
        this.persistFavoriteData();
        this.rebuildCategoryTabs();

        Form selected = this.getSelected();
        String query = this.search.getText();
        this.setupForms(BBSModClient.getFormCategories());
        this.applySearchNow(query);
        this.restoreSelectedIfPresent(selected);

        return true;
    }

    private boolean editCustomCategory(String categoryId, UIFavoriteCategoryOverlayPanel.FavoriteCategoryData data)
    {
        FavoriteCategory current = this.customFavoriteCategories.get(categoryId);

        if (current == null)
        {
            return false;
        }

        String name = data.name == null ? "" : data.name.trim();

        if (name.isEmpty())
        {
            return false;
        }

        if (name.length() > MAX_CATEGORY_NAME_LENGTH)
        {
            name = name.substring(0, MAX_CATEGORY_NAME_LENGTH);
        }

        if (this.hasCategoryName(name, categoryId))
        {
            UIOverlay.addOverlay(this.getContext(), new UIMessageOverlayPanel(
                UIKeys.GENERAL_WARNING,
                UIKeys.FORMS_LIST_CATEGORIES_CREATE_EXISTS.format(name)
            ), 320, 120);

            return false;
        }

        FavoriteCategory updated = new FavoriteCategory(categoryId, name, data.iconName, data.color & Colors.RGB);

        this.customFavoriteCategories.put(categoryId, updated);
        this.persistFavoriteData();
        this.rebuildCategoryTabs();

        Form selected = this.getSelected();
        String query = this.search.getText();
        this.setupForms(BBSModClient.getFormCategories());
        this.applySearchNow(query);
        this.restoreSelectedIfPresent(selected);

        return true;
    }

    private boolean hasCategoryName(String name)
    {
        return this.hasCategoryName(name, null);
    }

    private boolean hasCategoryName(String name, String ignoreCategoryId)
    {
        for (FavoriteCategory category : this.customFavoriteCategories.values())
        {
            if (!Objects.equals(category.id, ignoreCategoryId) && category.name.equalsIgnoreCase(name))
            {
                return true;
            }
        }

        return false;
    }

    private String getCategoryTabTitle(String name)
    {
        if (name == null)
        {
            return "";
        }

        String trimmed = name.trim();

        if (trimmed.length() <= MAX_TAB_TITLE_LENGTH)
        {
            return trimmed;
        }

        return trimmed.substring(0, MAX_TAB_TITLE_LENGTH) + "...";
    }

    private FavoriteCategory findCustomCategoryForKey(String key)
    {
        if (key == null)
        {
            return null;
        }

        for (FavoriteCategory category : this.customFavoriteCategories.values())
        {
            Set<String> forms = this.customCategoryForms.get(category.id);

            if (forms != null && forms.contains(key))
            {
                return category;
            }
        }

        return null;
    }

    private List<FavoriteMarker> getCategoryPickOptions()
    {
        List<FavoriteMarker> options = new ArrayList<>();

        options.add(new FavoriteMarker(FAVORITES_CATEGORY_ID, UIKeys.FORMS_LIST_TAB_FAVORITES.get(), Icons.FIVE_STAR, Colors.YELLOW | Colors.A100));

        for (FavoriteCategory category : this.customFavoriteCategories.values())
        {
            options.add(new FavoriteMarker(category.id, category.name, this.getIconByName(category.icon), (category.color & Colors.RGB) | Colors.A100));
        }

        return options;
    }

    private void persistFavoriteData()
    {
        BBSSettings.favoriteModelForms.set(new HashSet<>(this.favoriteModelForms));

        MapType root = new MapType();
        ListType categories = new ListType();
        MapType entries = new MapType();

        for (FavoriteCategory category : this.customFavoriteCategories.values())
        {
            MapType item = new MapType();

            item.putString("id", category.id);
            item.putString("name", category.name);
            item.putString("icon", category.icon);
            item.putInt("color", category.color);
            categories.add(item);
        }

        entries.put(FAVORITES_CATEGORY_ID, this.toList(this.favoriteModelForms));

        for (Map.Entry<String, Set<String>> entry : this.customCategoryForms.entrySet())
        {
            if (this.customFavoriteCategories.containsKey(entry.getKey()))
            {
                entries.put(entry.getKey(), this.toList(entry.getValue()));
            }
        }

        root.putInt("version", 1);
        root.put("categories", categories);
        root.put("entries", entries);
        root.put("hidden_previews", this.toList(this.hiddenCategoryPreviews));
        BBSSettings.favoriteFormCategoriesData.set(DataToString.toString(root));
        this.syncedFavoriteCategoriesData = BBSSettings.favoriteFormCategoriesData.get();
        this.syncedFavoriteModels.clear();
        this.syncedFavoriteModels.addAll(this.favoriteModelForms);
    }

    private void syncFavoriteData()
    {
        String categoriesData = BBSSettings.favoriteFormCategoriesData.get();
        Set<String> sharedFavorites = BBSSettings.favoriteModelForms.get();

        if (Objects.equals(this.syncedFavoriteCategoriesData, categoriesData) && this.syncedFavoriteModels.equals(sharedFavorites))
        {
            return;
        }

        Form selected = this.getSelected();
        String query = this.search.getText();

        this.loadFavoriteDataFromSettings();
        this.rebuildCategoryTabs();

        if (this.isFavoritesOnly())
        {
            this.setupForms(BBSModClient.getFormCategories());
            this.applySearchNow(query);
            this.restoreSelectedIfPresent(selected);
            this.forms.resize();
            this.resize();
        }
    }

    private void loadFavoriteDataFromSettings()
    {
        this.favoriteModelForms.clear();
        this.favoriteModelForms.addAll(BBSSettings.favoriteModelForms.get());
        this.customFavoriteCategories.clear();
        this.customCategoryForms.clear();
        this.hiddenCategoryPreviews.clear();

        String raw = BBSSettings.favoriteFormCategoriesData.get();
        MapType root = DataToString.mapFromString(raw);

        if (root != null && root.has("categories", BaseType.TYPE_LIST))
        {
            ListType categories = root.getList("categories");

            for (BaseType type : categories)
            {
                if (!type.isMap())
                {
                    continue;
                }

                MapType item = type.asMap();
                String id = item.getString("id", "").trim().toLowerCase(Locale.ROOT);
                String name = item.getString("name", "").trim();

                if (id.isEmpty() || name.isEmpty() || FAVORITES_CATEGORY_ID.equals(id))
                {
                    continue;
                }

                FavoriteCategory category = new FavoriteCategory(id, name, item.getString("icon", "five_star"), item.getInt("color", BBSSettings.primaryColor.get()) & Colors.RGB);
                this.customFavoriteCategories.put(category.id, category);
                this.customCategoryForms.put(category.id, new HashSet<>());
            }
        }

        if (root != null && root.has("entries", BaseType.TYPE_MAP))
        {
            MapType entries = root.getMap("entries");

            if (entries.has(FAVORITES_CATEGORY_ID, BaseType.TYPE_LIST))
            {
                this.favoriteModelForms.clear();
                this.favoriteModelForms.addAll(this.fromList(entries.getList(FAVORITES_CATEGORY_ID)));
            }

            for (Map.Entry<String, BaseType> entry : entries)
            {
                if (!entry.getValue().isList() || FAVORITES_CATEGORY_ID.equals(entry.getKey()))
                {
                    continue;
                }

                if (this.customFavoriteCategories.containsKey(entry.getKey()))
                {
                    this.customCategoryForms.put(entry.getKey(), this.fromList(entry.getValue().asList()));
                }
            }
        }

        if (root != null && root.has("hidden_previews", BaseType.TYPE_LIST))
        {
            this.hiddenCategoryPreviews.addAll(this.fromList(root.getList("hidden_previews")));
        }

        if (this.activeFavoriteCategoryId != null && !FAVORITES_CATEGORY_ID.equals(this.activeFavoriteCategoryId) && !this.customFavoriteCategories.containsKey(this.activeFavoriteCategoryId))
        {
            this.activeFavoriteCategoryId = FAVORITES_CATEGORY_ID;
        }

        this.syncedFavoriteCategoriesData = raw;
        this.syncedFavoriteModels.clear();
        this.syncedFavoriteModels.addAll(this.favoriteModelForms);
    }

    private Set<String> getFormsForCategory(String categoryId, boolean create)
    {
        if (categoryId == null)
        {
            return null;
        }

        if (FAVORITES_CATEGORY_ID.equals(categoryId))
        {
            return this.favoriteModelForms;
        }

        if (!this.customFavoriteCategories.containsKey(categoryId))
        {
            return null;
        }

        if (create)
        {
            return this.customCategoryForms.computeIfAbsent(categoryId, (id) -> new HashSet<>());
        }

        return this.customCategoryForms.get(categoryId);
    }

    private String getCurrentMarkerCategoryId()
    {
        return this.activeFavoriteCategoryId == null ? FAVORITES_CATEGORY_ID : this.activeFavoriteCategoryId;
    }

    private Icon getIconByName(String iconName)
    {
        return Icons.ICONS.getOrDefault(iconName, Icons.FIVE_STAR);
    }

    private String generateUniqueCategoryId(String name)
    {
        StringBuilder slug = new StringBuilder();

        for (char c : name.toLowerCase(Locale.ROOT).toCharArray())
        {
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9'))
            {
                slug.append(c);
            }
            else if (slug.length() == 0 || slug.charAt(slug.length() - 1) != '_')
            {
                slug.append('_');
            }
        }

        String base = slug.toString().replaceAll("^_+|_+$", "");

        if (base.isEmpty())
        {
            base = "category";
        }

        String id = base;
        int index = 2;

        while (this.customFavoriteCategories.containsKey(id) || FAVORITES_CATEGORY_ID.equals(id))
        {
            id = base + "_" + index;
            index += 1;
        }

        return id;
    }

    private ListType toList(Set<String> values)
    {
        ListType list = new ListType();

        for (String value : values)
        {
            list.addString(value);
        }

        return list;
    }

    private Set<String> fromList(ListType list)
    {
        Set<String> values = new HashSet<>();

        for (BaseType type : list)
        {
            if (type.isString())
            {
                values.add(type.asString());
            }
        }

        return values;
    }

    private String getFavoriteKey(Form form)
    {
        if (form == null)
        {
            return null;
        }

        if (form instanceof ModelForm modelForm)
        {
            String model = modelForm.model.get();

            if (model == null || model.isBlank())
            {
                return null;
            }

            return model.toLowerCase();
        }

        if (form instanceof ParticleForm particleForm)
        {
            String effect = particleForm.effect.get();

            if (effect == null || effect.isBlank())
            {
                return null;
            }

            return "particle:" + effect.toLowerCase();
        }

        String formId = form.getFormId();

        if (formId == null || formId.isBlank())
        {
            return null;
        }

        return "form:" + formId.toLowerCase();
    }

    private boolean isFavoritesFeatureEnabled()
    {
        return this.palette instanceof UIFormPalette formPalette && formPalette.isImmersive();
    }

    private void applyFavoritesLayout(boolean enabled)
    {
        if (this.favoritesUiVisible == enabled)
        {
            return;
        }

        this.favoritesUiVisible = enabled;
        this.tabsBar.setVisible(enabled);
        this.tabs.setVisible(enabled);

        if (!enabled)
        {
            this.activeFavoriteCategoryId = null;
            this.notifyFavoriteCategoryChanged();
        }

        int barY = enabled ? FAVORITES_TOP_BAR_HEIGHT : 0;
        int topOffset = ACTIONS_BAR_HEIGHT + (enabled ? FAVORITES_TOP_BAR_HEIGHT + FAVORITES_TOP_BAR_BOTTOM_SPACING : 0);

        this.bar.relative(this).x(0).y(barY).w(1F).h(ACTIONS_BAR_HEIGHT);
        this.layoutActionBar();
        this.bar.resize();

        this.forms.relative(this).x(0).y(topOffset).w(1F).h(1F, -topOffset);
        this.categoryCardsView.relative(this).x(0).y(topOffset).w(1F).h(1F, -topOffset);
        this.forms.resize();
        this.categoryCardsView.resize();
        this.resize();
    }

    private void layoutActionBar()
    {
        int rightOffset = 0;

        if (this.close.getParent() == this.bar)
        {
            this.close.relative(this.bar).x(1F, -20 - rightOffset).y(0).w(20).h(ACTIONS_BAR_HEIGHT);
            rightOffset += 20;
        }

        if (this.edit.getParent() == this.bar)
        {
            this.edit.relative(this.bar).x(1F, -20 - rightOffset).y(0).w(20).h(ACTIONS_BAR_HEIGHT);
            rightOffset += 20;
        }

        for (IUIElement child : this.bar.getChildren())
        {
            if (!(child instanceof UIIcon))
            {
                continue;
            }

            UIIcon icon = (UIIcon) child;

            if (icon == this.edit || icon == this.close)
            {
                continue;
            }

            icon.relative(this.bar).x(1F, -20 - rightOffset).y(0).w(20).h(ACTIONS_BAR_HEIGHT);
            rightOffset += 20;
        }

        int searchX = SELECTED_INFO_WIDTH + 6;
        this.search.relative(this.bar).x(searchX).y(0).w(1F, -(searchX + rightOffset)).h(ACTIONS_BAR_HEIGHT);
    }

    public void setFavoriteCategoryChangedListener(Consumer<String> callback)
    {
        this.favoriteCategoryChanged = callback;
    }

    public String getActiveFavoriteCategoryId()
    {
        return this.activeFavoriteCategoryId;
    }

    public boolean hasFavoriteCategory(String categoryId)
    {
        if (categoryId == null || FAVORITES_CATEGORY_ID.equals(categoryId))
        {
            return true;
        }

        return this.customFavoriteCategories.containsKey(categoryId);
    }

    public void setActiveFavoriteCategoryWithFallback(String categoryId)
    {
        String target = this.hasFavoriteCategory(categoryId) ? categoryId : null;

        this.setActiveFavoriteCategory(target);
    }

    private void notifyFavoriteCategoryChanged()
    {
        if (this.favoriteCategoryChanged != null)
        {
            this.favoriteCategoryChanged.accept(this.activeFavoriteCategoryId);
        }
    }

    private void search(String search)
    {
        this.pendingSearchQuery = search == null ? "" : search.trim();
        this.pendingSearchDeadline = System.currentTimeMillis() + SEARCH_DEBOUNCE_MS;
    }

    private void applySearchNow(String search)
    {
        search = search.trim();
        this.pendingSearchQuery = search;
        this.appliedSearchQuery = search;
        this.pendingSearchDeadline = -1L;

        for (UIFormCategory category : this.categories)
        {
            category.search(search);
        }

        this.invalidateCategoryCards();
    }

    private void flushPendingSearch()
    {
        if (this.pendingSearchDeadline >= 0L && System.currentTimeMillis() >= this.pendingSearchDeadline)
        {
            this.applySearchNow(this.pendingSearchQuery);
        }
    }

    private void invalidateCategoryCards()
    {
        double scroll = this.categoryCardsView.scroll.getScroll();
        this.categoryCards.invalidateCache();
        this.categoryCardsView.resize();
        this.categoryCardsView.scroll.setScroll(scroll);
    }

    private String getCategoryPreviewKey(UIFormCategory category)
    {
        if (category == null || category.category == null)
        {
            return "";
        }

        String id = category.category.visible.getId();

        if (id != null)
        {
            id = id.trim().toLowerCase(Locale.ROOT);
        }

        if (id != null && !id.isEmpty())
        {
            return id;
        }

        String title = category.category.getProcessedTitle();

        return title == null ? "" : title.trim().toLowerCase(Locale.ROOT);
    }

    private Set<String> getAvailableCategoryPreviewKeys()
    {
        Set<String> keys = new HashSet<>();

        for (UIFormCategory category : this.categories)
        {
            String key = this.getCategoryPreviewKey(category);

            if (!key.isEmpty())
            {
                keys.add(key);
            }
        }

        return keys;
    }

    private boolean isCategoryPreviewVisible(UIFormCategory category)
    {
        return !this.hiddenCategoryPreviews.contains(this.getCategoryPreviewKey(category));
    }

    private void toggleCategoryPreview(UIFormCategory category)
    {
        String key = this.getCategoryPreviewKey(category);

        if (key.isEmpty())
        {
            return;
        }

        if (this.hiddenCategoryPreviews.contains(key))
        {
            this.hiddenCategoryPreviews.remove(key);
        }
        else
        {
            this.hiddenCategoryPreviews.add(key);
        }

        this.persistFavoriteData();
        this.invalidateCategoryCards();
    }

    private boolean areAllCategoryPreviewsVisible()
    {
        Set<String> keys = this.getAvailableCategoryPreviewKeys();

        if (keys.isEmpty())
        {
            return true;
        }

        for (String key : keys)
        {
            if (this.hiddenCategoryPreviews.contains(key))
            {
                return false;
            }
        }

        return true;
    }

    private void toggleAllCategoryPreviews(UIIcon button)
    {
        Set<String> keys = this.getAvailableCategoryPreviewKeys();

        if (keys.isEmpty())
        {
            return;
        }

        if (this.areAllCategoryPreviewsVisible())
        {
            this.hiddenCategoryPreviews.addAll(keys);
        }
        else
        {
            this.hiddenCategoryPreviews.removeAll(keys);
        }

        this.persistFavoriteData();
        this.invalidateCategoryCards();
    }

    private void restoreSelectedIfPresent(Form form)
    {
        if (form == null)
        {
            this.deselect();

            return;
        }

        this.deselect();

        for (UIFormCategory category : this.categories)
        {
            List<Form> forms = category.getForms();
            int index = forms.indexOf(form);

            if (index >= 0)
            {
                category.select(forms.get(index), false);

                return;
            }
        }
    }

    private void edit(UIIcon b)
    {
        this.palette.toggleEditor();
    }

    private void close(UIIcon b)
    {
        this.palette.exit();
    }

    private void openCategoryPopup(UIFormCategory category)
    {
        if (category == null)
        {
            return;
        }

        this.closeCategoryPopup();

        UIContext context = this.getContext();

        if (context == null || context.menu == null || context.menu.overlay == null)
        {
            return;
        }

        UIElement overlay = context.menu.overlay;
        UIElement[] contentHolder = new UIElement[1];

        UIElement popup = new UIElement()
        {
            @Override
            public boolean subMouseClicked(UIContext context)
            {
                if (context.mouseButton == 0 && contentHolder[0] != null && !contentHolder[0].area.isInside(context))
                {
                    UIFormList.this.closeCategoryPopup();

                    return true;
                }

                return super.subMouseClicked(context);
            }

            @Override
            public boolean subKeyPressed(UIContext context)
            {
                if (context.isPressed(Keys.CLOSE))
                {
                    UIFormList.this.closeCategoryPopup();

                    return true;
                }

                return super.subKeyPressed(context);
            }

            @Override
            public void render(UIContext context)
            {
                this.area.render(context.batcher, Colors.A50);
                super.render(context);
            }
        };
        popup.full(overlay);
        popup.markContainer().eventPropagataion(EventPropagation.BLOCK);

        UIElement content = new UIElement()
        {
            @Override
            public void render(UIContext context)
            {
                this.area.render(context.batcher, Colors.A25);
                context.batcher.outline(this.area.x - 1, this.area.y - 1, this.area.ex() + 1, this.area.ey() + 1, Colors.A100);
                context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0xff000000 | BBSSettings.primaryColor.get());
                context.batcher.textCard(category.category.getProcessedTitle(), this.area.x + 6, this.area.y + 6);
                super.render(context);
            }
        };
        content.relative(popup).set(20, 20, 0, 0).w(1F, -40).h(1F, -40);
        contentHolder[0] = content;

        UIIcon closePopup = new UIIcon(Icons.CLOSE, (button) -> this.closeCategoryPopup());
        closePopup.relative(content).x(1F, -20).y(0).w(20).h(20).tooltip(UIKeys.GENERAL_CLOSE, Direction.LEFT);

        UIScrollView popupScroll = UI.scrollView(0, 0);
        popupScroll.relative(content).x(0).y(20).w(1F).h(1F, -20);
        popupScroll.scroll.cancelScrolling();

        UICategoryPopupGrid popupGrid = new UICategoryPopupGrid(category);
        popupGrid.relative(popupScroll).x(0).y(0).w(1F).h(0);
        popupScroll.add(popupGrid);
        popupGrid.h(1);

        content.add(closePopup, popupScroll);
        popup.add(content);
        overlay.add(popup);
        popup.resize();
        content.resize();
        popupScroll.resize();
        popupGrid.resize();
        this.categoryPopup = popup;
    }

    private void closeCategoryPopup()
    {
        if (this.categoryPopup != null)
        {
            this.categoryPopup.removeFromParent();
            this.categoryPopup = null;
        }
    }

    public void selectCategory(UIFormCategory category, Form form, boolean notify)
    {
        this.deselect();

        category.selected = form;

        if (notify)
        {
            this.palette.accept(form);
        }
    }

    public void deselect()
    {
        for (UIFormCategory category : this.categories)
        {
            category.selected = null;
        }
    }

    public UIFormCategory getSelectedCategory()
    {
        for (UIFormCategory category : this.categories)
        {
            if (category.selected != null)
            {
                return category;
            }
        }

        return null;
    }

    public Form getSelected()
    {
        UIFormCategory category = this.getSelectedCategory();

        return category == null ? null : category.selected;
    }

    public void setSelected(Form form)
    {
        boolean found = false;

        this.deselect();

        for (UIFormCategory category : this.categories)
        {
            int index = category.category.getForms().indexOf(form);

            if (index == -1)
            {
                category.selected = null;
            }
            else
            {
                found = true;

                category.select(category.category.getForms().get(index), false);
            }
        }

        if (!found && form != null)
        {
            Form copy = FormUtils.copy(form);

            this.recent.category.addForm(copy);
            this.recent.select(copy, false);
        }
    }

    public boolean handleFormDrop(UIFormCategory source, int sourceIndex, int mouseX, int mouseY)
    {
        for (UIFormCategory category : this.categories)
        {
            if (category != source && category.area.isInside(mouseX, mouseY) && category.category instanceof UserFormCategory)
            {
                int index = category.getIndexAt(mouseX, mouseY);
                
                if (index != -1)
                {
                    Form form = source.category.getForms().get(sourceIndex);
                    
                    ((UserFormCategory) category.category).addForm(index, form);
                    source.category.removeForm(form);
                    
                    return true;
                }
            }
        }
        
        return false;
    }

    @Override
    public void render(UIContext context)
    {
        FormCategories categories = BBSModClient.getFormCategories();
        this.applyFavoritesLayout(this.isFavoritesFeatureEnabled());
        this.layoutActionBar();
        this.syncFavoriteData();
        this.updateTabs();
        this.flushPendingSearch();

        if (this.lastScroll >= 0)
        {
            this.forms.scroll.scrollTo(this.lastScroll);

            this.lastScroll = -1;
        }

        if (this.lastUpdate != categories.getLastUpdate())
        {
            this.lastScroll = (int) this.forms.scroll.getScroll();

            Form selected = this.getSelected();

            this.setupForms(categories);
            this.setSelected(selected);
        }

        DiffuseLighting.enableGuiDepthLighting();

        super.render(context);

        DiffuseLighting.disableGuiDepthLighting();

    }

    private class UICategoryCardsGrid extends UIElement
    {
        private final List<UIFormCategory> filteredCategories = new ArrayList<>();
        private final Map<UIFormCategory, List<Form>> previewCache = new HashMap<>();
        private boolean dirty = true;
        private int lastHeight;
        private int cachedPerRow = 1;

        public void invalidateCache()
        {
            this.dirty = true;
            this.lastHeight = 0;
            this.previewCache.clear();
        }

        @Override
        public boolean subMouseClicked(UIContext context)
        {
            if (context.mouseButton != 0 || !this.area.isInside(context))
            {
                return false;
            }

            CategoryCell cell = this.getCategoryCellAt(context.mouseX, context.mouseY);

            if (cell == null)
            {
                return false;
            }

            if (this.isToggleButtonAt(context.mouseX, context.mouseY, cell.x, cell.y))
            {
                UIFormList.this.toggleCategoryPreview(cell.category);

                return true;
            }

            UIFormList.this.openCategoryPopup(cell.category);

            return true;
        }

        @Override
        public void render(UIContext context)
        {
            this.rebuildIfNeeded();

            int step = CATEGORY_CARD_HEIGHT + CATEGORY_CARD_GAP;
            int rows = (this.filteredCategories.size() + this.cachedPerRow - 1) / this.cachedPerRow;
            int contentHeight = CATEGORY_CARD_GAP + rows * step;

            if (this.lastHeight != contentHeight)
            {
                this.lastHeight = contentHeight;
                this.h(contentHeight);

                if (this.getParent() != null)
                {
                    this.getParent().resize();
                }
            }

            if (this.filteredCategories.isEmpty())
            {
                return;
            }

            int scrollY = 0;
            int viewportHeight = this.area.h;

            if (this.getParent() instanceof UIScrollView scrollView)
            {
                scrollY = (int) scrollView.scroll.getScroll();
                viewportHeight = scrollView.area.h;
            }

            int top = Math.max(0, scrollY - CATEGORY_CARD_GAP);
            int bottom = Math.max(0, scrollY + viewportHeight - CATEGORY_CARD_GAP);
            int rowStart = Math.max(0, top / step - CATEGORY_VIRTUALIZATION_BUFFER_ROWS);
            int rowEnd = Math.min(rows - 1, bottom / step + CATEGORY_VIRTUALIZATION_BUFFER_ROWS);

            for (int row = rowStart; row <= rowEnd; row++)
            {
                for (int column = 0; column < this.cachedPerRow; column++)
                {
                    int index = row * this.cachedPerRow + column;

                    if (index >= this.filteredCategories.size())
                    {
                        break;
                    }

                    UIFormCategory category = this.filteredCategories.get(index);
                    int x = this.area.x + CATEGORY_CARD_GAP + column * (CATEGORY_CARD_WIDTH + CATEGORY_CARD_GAP);
                    int y = this.area.y + CATEGORY_CARD_GAP + row * step;
                    this.renderCategoryCard(context, category, x, y);
                }
            }
        }

        private void rebuildIfNeeded()
        {
            int perRow = Math.max(1, (this.area.w - CATEGORY_CARD_GAP) / (CATEGORY_CARD_WIDTH + CATEGORY_CARD_GAP));

            if (!this.dirty && this.cachedPerRow == perRow)
            {
                return;
            }

            this.cachedPerRow = perRow;
            this.filteredCategories.clear();

            for (UIFormCategory category : UIFormList.this.categories)
            {
                List<Form> forms = category.getForms();

                if (!forms.isEmpty())
                {
                    this.filteredCategories.add(category);
                    this.previewCache.put(category, forms);
                }
            }

            this.dirty = false;
        }

        private CategoryCell getCategoryCellAt(int mouseX, int mouseY)
        {
            this.rebuildIfNeeded();

            int step = CATEGORY_CARD_HEIGHT + CATEGORY_CARD_GAP;

            for (int i = 0; i < this.filteredCategories.size(); i++)
            {
                int row = i / this.cachedPerRow;
                int column = i % this.cachedPerRow;
                int x = this.area.x + CATEGORY_CARD_GAP + column * (CATEGORY_CARD_WIDTH + CATEGORY_CARD_GAP);
                int y = this.area.y + CATEGORY_CARD_GAP + row * step;

                if (mouseX >= x && mouseX < x + CATEGORY_CARD_WIDTH && mouseY >= y && mouseY < y + CATEGORY_CARD_HEIGHT)
                {
                    return new CategoryCell(this.filteredCategories.get(i), x, y);
                }
            }

            return null;
        }

        private boolean isToggleButtonAt(int mouseX, int mouseY, int cardX, int cardY)
        {
            int iconX = cardX + CATEGORY_CARD_WIDTH - CATEGORY_PREVIEW_TOGGLE_SIZE - 6;
            int iconY = cardY + 4;

            return mouseX >= iconX && mouseX < iconX + CATEGORY_PREVIEW_TOGGLE_SIZE && mouseY >= iconY && mouseY < iconY + CATEGORY_PREVIEW_TOGGLE_SIZE;
        }

        private void renderCategoryCard(UIContext context, UIFormCategory category, int x, int y)
        {
            List<Form> forms = this.previewCache.getOrDefault(category, category.getForms());
            boolean previewsVisible = UIFormList.this.isCategoryPreviewVisible(category);
            UIFormCategory selectedCategory = UIFormList.this.getSelectedCategory();
            boolean selected = selectedCategory == category;
            int baseColor = selected ? (Colors.A50 | BBSSettings.primaryColor.get()) : Colors.A25;

            context.batcher.box(x, y, x + CATEGORY_CARD_WIDTH, y + CATEGORY_CARD_HEIGHT, baseColor);
            context.batcher.outline(x, y, x + CATEGORY_CARD_WIDTH, y + CATEGORY_CARD_HEIGHT, Colors.A100);

            String title = context.batcher.getFont().limitToWidth(category.category.getProcessedTitle(), CATEGORY_CARD_WIDTH - 28);
            context.batcher.textShadow(title, x + 6, y + 4);

            int iconX = x + CATEGORY_CARD_WIDTH - CATEGORY_PREVIEW_TOGGLE_SIZE - 6;
            int iconY = y + 4;
            int hoverColor = this.isToggleButtonAt(context.mouseX, context.mouseY, x, y) ? Colors.A100 : Colors.A50;
            context.batcher.box(iconX - 1, iconY - 1, iconX + CATEGORY_PREVIEW_TOGGLE_SIZE + 1, iconY + CATEGORY_PREVIEW_TOGGLE_SIZE + 1, hoverColor);
            context.batcher.icon(previewsVisible ? Icons.VISIBLE : Icons.INVISIBLE, Colors.WHITE, iconX + CATEGORY_PREVIEW_TOGGLE_SIZE / 2, iconY + CATEGORY_PREVIEW_TOGGLE_SIZE / 2, 0.5F, 0.5F);

            if (previewsVisible)
            {
                int previewAreaX = x + CATEGORY_PREVIEW_PADDING;
                int previewAreaY = y + 20;
                int previewAreaW = CATEGORY_CARD_WIDTH - CATEGORY_PREVIEW_PADDING * 2;
                int previewAreaH = CATEGORY_CARD_HEIGHT - 28;
                int cellW = (previewAreaW - CATEGORY_PREVIEW_GAP * (CATEGORY_PREVIEW_COLUMNS - 1)) / CATEGORY_PREVIEW_COLUMNS;
                int cellH = (previewAreaH - CATEGORY_PREVIEW_GAP * (CATEGORY_PREVIEW_ROWS - 1)) / CATEGORY_PREVIEW_ROWS;
                int maxPreview = CATEGORY_PREVIEW_COLUMNS * CATEGORY_PREVIEW_ROWS;
                int shown = Math.min(maxPreview, forms.size());

                for (int i = 0; i < shown; i++)
                {
                    int px = previewAreaX + (i % CATEGORY_PREVIEW_COLUMNS) * (cellW + CATEGORY_PREVIEW_GAP);
                    int py = previewAreaY + (i / CATEGORY_PREVIEW_COLUMNS) * (cellH + CATEGORY_PREVIEW_GAP);

                    context.batcher.box(px, py, px + cellW, py + cellH, Colors.A25);
                    context.batcher.clip(px, py, cellW, cellH, context);
                    FormUtilsClient.renderUI(forms.get(i), context, px, py, px + cellW, py + cellH);
                    context.batcher.unclip(context);
                    context.batcher.outline(px, py, px + cellW, py + cellH, Colors.A50);
                }

                if (forms.size() > maxPreview)
                {
                    String count = "+" + (forms.size() - maxPreview);
                    int width = context.batcher.getFont().getWidth(count) + 6;
                    int badgeX = x + CATEGORY_CARD_WIDTH - width - 6;
                    int badgeY = y + CATEGORY_CARD_HEIGHT - 16;

                    context.batcher.box(badgeX, badgeY, badgeX + width, badgeY + 12, Colors.A100 | 0x1a1a1a);
                    context.batcher.textShadow(count, badgeX + 3, badgeY + 2);
                }
            }
            else
            {
                int hiddenIconX = x + (CATEGORY_CARD_WIDTH - CATEGORY_HIDDEN_ICON_SIZE) / 2;
                int hiddenIconY = y + (CATEGORY_CARD_HEIGHT - CATEGORY_HIDDEN_ICON_SIZE) / 2 + 6;
                context.batcher.texturedBox(
                    BBSModClient.getTextures().getTexture(Icons.CROSSED_OUT_EYE.texture),
                    Colors.LIGHTEST_GRAY,
                    hiddenIconX,
                    hiddenIconY,
                    CATEGORY_HIDDEN_ICON_SIZE,
                    CATEGORY_HIDDEN_ICON_SIZE,
                    Icons.CROSSED_OUT_EYE.x,
                    Icons.CROSSED_OUT_EYE.y,
                    Icons.CROSSED_OUT_EYE.x + Icons.CROSSED_OUT_EYE.w,
                    Icons.CROSSED_OUT_EYE.y + Icons.CROSSED_OUT_EYE.h,
                    Icons.CROSSED_OUT_EYE.textureW,
                    Icons.CROSSED_OUT_EYE.textureH
                );
            }
        }

        private class CategoryCell
        {
            private final UIFormCategory category;
            private final int x;
            private final int y;

            private CategoryCell(UIFormCategory category, int x, int y)
            {
                this.category = category;
                this.x = x;
                this.y = y;
            }
        }
    }

    private class UICategoryPopupGrid extends UIElement
    {
        private final UIFormCategory category;
        private int lastHeight;

        public UICategoryPopupGrid(UIFormCategory category)
        {
            this.category = category;
        }

        @Override
        public boolean subMouseClicked(UIContext context)
        {
            if (context.mouseButton != 0 || !this.area.isInside(context))
            {
                return false;
            }

            List<Form> forms = this.category.getForms();

            if (forms.isEmpty())
            {
                return false;
            }

            int x = context.mouseX - this.area.x;
            int y = context.mouseY - this.area.y;
            int perRow = Math.max(1, this.area.w / POPUP_CELL_WIDTH);
            int index = x / POPUP_CELL_WIDTH + (y / POPUP_CELL_HEIGHT) * perRow;

            if (index >= 0 && index < forms.size())
            {
                this.category.select(forms.get(index), true);
                UIFormList.this.closeCategoryPopup();

                return true;
            }

            return false;
        }

        @Override
        public void render(UIContext context)
        {
            List<Form> forms = this.category.getForms();
            int perRow = Math.max(1, this.area.w / POPUP_CELL_WIDTH);
            int h = 0;
            int x = 0;
            int i = 0;

            for (Form form : forms)
            {
                if (i == perRow)
                {
                    h += POPUP_CELL_HEIGHT;
                    x = 0;
                    i = 0;
                }

                int cx = this.area.x + x;
                int cy = this.area.y + h;
                boolean selected = this.category.selected == form;

                if (selected)
                {
                    context.batcher.box(cx, cy, cx + POPUP_CELL_WIDTH, cy + POPUP_CELL_HEIGHT, Colors.A50 | BBSSettings.primaryColor.get());
                    context.batcher.outline(cx, cy, cx + POPUP_CELL_WIDTH, cy + POPUP_CELL_HEIGHT, Colors.A50 | BBSSettings.primaryColor.get(), 2);
                }
                else
                {
                    context.batcher.box(cx, cy, cx + POPUP_CELL_WIDTH, cy + POPUP_CELL_HEIGHT, Colors.A25);
                }

                context.batcher.clip(cx, cy, POPUP_CELL_WIDTH, POPUP_CELL_HEIGHT, context);
                FormUtilsClient.renderUI(form, context, cx, cy, cx + POPUP_CELL_WIDTH, cy + POPUP_CELL_HEIGHT);
                context.batcher.unclip(context);
                context.batcher.outline(cx, cy, cx + POPUP_CELL_WIDTH, cy + POPUP_CELL_HEIGHT, Colors.A50);

                x += POPUP_CELL_WIDTH;
                i += 1;
            }

            h += POPUP_CELL_HEIGHT;

            if (this.lastHeight != h)
            {
                this.lastHeight = h;
                this.h(h);

                if (this.getParent() != null)
                {
                    this.getParent().resize();
                }
            }
        }
    }

    public static class FavoriteMarker
    {
        public final String categoryId;
        public final String name;
        public final Icon icon;
        public final int color;

        public FavoriteMarker(String categoryId, String name, Icon icon, int color)
        {
            this.categoryId = categoryId;
            this.name = name;
            this.icon = icon == null ? Icons.FIVE_STAR : icon;
            this.color = color;
        }
    }

    private static class UICategoryPickOverlayPanel extends UIOverlayPanel
    {
        private final List<FavoriteMarker> options;
        private final UICategoryPickList list;

        public UICategoryPickOverlayPanel(List<FavoriteMarker> options, Consumer<FavoriteMarker> callback)
        {
            super(UIKeys.FORMS_LIST_CATEGORIES_PICK_CATEGORY);

            this.options = options == null ? List.of() : options;
            this.list = new UICategoryPickList(this.options, (marker) ->
            {
                if (callback != null)
                {
                    callback.accept(marker);
                }

                this.close();
            });

            UISearchList<FavoriteMarker> searchList = new UISearchList<>(this.list);
            searchList.label(UIKeys.GENERAL_SEARCH).full(this.content).x(6).w(1F, -12).h(1F, -6);
            this.content.add(searchList);
        }

        public void setCurrent(String categoryId)
        {
            this.list.setCurrentById(categoryId);
        }
    }

    private static class UICategoryPickList extends UIList<FavoriteMarker>
    {
        private final Consumer<FavoriteMarker> onPicked;

        public UICategoryPickList(List<FavoriteMarker> options, Consumer<FavoriteMarker> callback)
        {
            super((values) ->
            {
                if (callback != null && !values.isEmpty())
                {
                    callback.accept(values.get(0));
                }
            });

            this.onPicked = callback;
            this.add(options == null ? List.of() : options);
            this.scroll.scrollItemSize = 20;
        }

        public void setCurrentById(String categoryId)
        {
            if (categoryId == null)
            {
                return;
            }

            for (int i = 0; i < this.list.size(); i++)
            {
                if (categoryId.equals(this.list.get(i).categoryId))
                {
                    this.setCurrentScroll(this.list.get(i));

                    return;
                }
            }
        }

        @Override
        public boolean subMouseClicked(UIContext context)
        {
            boolean result = super.subMouseClicked(context);

            if (result)
            {
                return true;
            }

            if (context.mouseButton != 0 || !this.area.isInside(context) || this.list.isEmpty())
            {
                return false;
            }

            int lastIndex = this.list.size() - 1;
            int index = this.scroll.getIndex(context.mouseX, context.mouseY);

            if (index == -2 || index > lastIndex)
            {
                this.setIndex(lastIndex);

                if (this.onPicked != null)
                {
                    this.onPicked.accept(this.list.get(lastIndex));
                }

                return true;
            }

            return false;
        }

        @Override
        public void renderListElement(UIContext context, FavoriteMarker marker, int i, int x, int y, boolean hover, boolean selected)
        {
            int base = marker == null ? BBSSettings.primaryColor.get() : marker.color & Colors.RGB;
            int stripeColor = Colors.A100 | base;
            int gradientTo = selected ? Colors.mulRGB(base, 0.9F) : base;

            if (selected)
            {
                context.batcher.box(x, y, x + this.area.w, y + this.scroll.scrollItemSize, Colors.A25);
            }

            context.batcher.box(x, y, x + 2, y + this.scroll.scrollItemSize, stripeColor);
            context.batcher.gradientHBox(x + 2, y, x + 24, y + this.scroll.scrollItemSize, Colors.A25 | base, gradientTo);

            this.renderElementPart(context, marker, i, x, y, hover, selected);
        }

        @Override
        protected void renderElementPart(UIContext context, FavoriteMarker marker, int i, int x, int y, boolean hover, boolean selected)
        {
            int textColor = hover || selected ? Colors.HIGHLIGHT : Colors.WHITE;

            if (marker == null)
            {
                super.renderElementPart(context, marker, i, x, y, hover, selected);
                return;
            }

            context.batcher.icon(marker.icon, Colors.WHITE, x + 12, y + this.scroll.scrollItemSize / 2, 0.5F, 0.5F);
            context.batcher.textShadow(marker.name, x + 24, y + (this.scroll.scrollItemSize - context.batcher.getFont().getHeight()) / 2, textColor);
        }

        @Override
        protected String elementToString(UIContext context, int i, FavoriteMarker marker)
        {
            return marker == null ? "" : marker.name;
        }
    }

    private static class FavoriteCategory
    {
        public final String id;
        public final String name;
        public final String icon;
        public final int color;

        public FavoriteCategory(String id, String name, String icon, int color)
        {
            this.id = id;
            this.name = name;
            this.icon = icon == null || icon.isBlank() ? "five_star" : icon;
            this.color = color;
        }
    }

}
