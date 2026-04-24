package mchorse.bbs_mod.ui.forms;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.FormCategories;
import mchorse.bbs_mod.forms.FormUtils;
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
    private static final int MAX_CATEGORY_NAME_LENGTH = 20;
    private static final int MAX_TAB_TITLE_LENGTH = 10;

    public IUIFormList palette;

    public UIScrollView forms;

    public UIElement tabsBar;
    public UIElement tabs;
    public UIElement bar;
    public UITextbox search;
    public UIButton allTab;
    public UIButton favoritesTab;
    public UIButton addCategoryTab;
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

    public UIFormList(IUIFormList palette)
    {
        this.palette = palette;
        this.loadFavoriteDataFromSettings();

        this.forms = UI.scrollView(0, 0);
        this.forms.scroll.cancelScrolling();
        this.tabsBar = new UIControlBar();
        this.tabs = new UIElement();
        this.bar = new UIElement()
        {
            @Override
            public void render(UIContext context)
            {
                context.batcher.getContext().getMatrices().push();
                context.batcher.getContext().getMatrices().translate(0, 0, 200);
                super.render(context);
                context.batcher.getContext().getMatrices().pop();
            }
        };
        this.search = new UITextbox(100, this::search).placeholder(UIKeys.FORMS_LIST_SEARCH);
        this.allTab = new UIIconTabButton(UIKeys.FORMS_LIST_TAB_ALL, Icons.LIST, (b) -> this.setActiveFavoriteCategory(null));
        this.favoritesTab = new UIIconTabButton(UIKeys.FORMS_LIST_TAB_FAVORITES, Icons.FIVE_STAR, (b) -> this.setActiveFavoriteCategory(FAVORITES_CATEGORY_ID));
        this.addCategoryTab = new UIIconTabButton(IKey.constant(""), Icons.ADD, (b) -> this.openCreateCategoryPanel());
        this.edit = new UIIcon(Icons.EDIT, this::edit);
        this.edit.tooltip(UIKeys.FORMS_LIST_EDIT, Direction.TOP);
        this.close = new UIIcon(Icons.CLOSE, this::close);

        this.forms.relative(this).x(0).y(0).w(1F).h(1F, -30);
        this.tabsBar.relative(this).x(0).y(0).w(1F).h(FAVORITES_TOP_BAR_HEIGHT);
        this.tabs.relative(this.tabsBar).x(10).y(0).w(0).h(FAVORITES_TOP_BAR_HEIGHT).row(0).resize();
        this.bar.relative(this).x(10).y(1F, -30).w(1F, -20).h(20).row().height(20);
        this.close.w(20);

        this.allTab.w(TAB_WIDTH_ALL).h(FAVORITES_TOP_BAR_HEIGHT);
        this.favoritesTab.w(TAB_WIDTH_FAVORITES).h(FAVORITES_TOP_BAR_HEIGHT);
        this.addCategoryTab.w(TAB_WIDTH_ADD).h(FAVORITES_TOP_BAR_HEIGHT);
        this.addCategoryTab.background(false);
        this.rebuildCategoryTabs();
        this.tabsBar.add(this.tabs);
        this.tabsBar.setVisible(false);
        this.bar.add(this.search, this.edit, this.close);
        this.add(this.forms, this.bar, this.tabsBar);

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
        this.search(query);
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
        this.search(query);
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
        this.search(query);
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
        this.search(query);
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
            this.search(query);
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

        if (!enabled)
        {
            this.activeFavoriteCategoryId = null;
            this.notifyFavoriteCategoryChanged();
        }

        this.forms.relative(this).x(0).y(enabled ? FAVORITES_TOP_BAR_HEIGHT + FAVORITES_TOP_BAR_BOTTOM_SPACING : 0).w(1F).h(1F, enabled ? -(FAVORITES_TOP_BAR_HEIGHT + FAVORITES_TOP_BAR_BOTTOM_SPACING + 30) : -30);
        this.forms.resize();
        this.resize();
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
        search = search.trim();

        for (UIFormCategory category : this.categories)
        {
            category.search(search);
        }
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
        this.syncFavoriteData();
        this.updateTabs();

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

        /* Render form's display name and ID */
        Form selected = this.getSelected();

        if (selected != null)
        {
            String displayName = selected.getDisplayName();
            String id = selected.getFormId();
            FontRenderer font = context.batcher.getFont();

            int w = Math.max(font.getWidth(displayName), font.getWidth(id));
            int x = this.search.area.x;
            int y = this.search.area.y - 24;

            context.batcher.box(x, y, x + w + 8, this.search.area.y, Colors.A50);
            context.batcher.textShadow(displayName, x + 4, y + 4);
            context.batcher.textShadow(id, x + 4, y + 14, Colors.LIGHTEST_GRAY);
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
