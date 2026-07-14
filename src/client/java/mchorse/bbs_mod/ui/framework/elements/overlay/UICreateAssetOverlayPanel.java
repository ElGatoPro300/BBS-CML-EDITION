package mchorse.bbs_mod.ui.framework.elements.overlay;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.ContentType;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;

import java.util.function.Consumer;

public class UICreateAssetOverlayPanel extends UIOverlayPanel
{
    public final UILabel typeLabel;
    public final UILabel typeValue;
    public final UILabel nameLabel;
    public final UITextbox nameField;
    public final UILabel folderLabel;
    public final UIButton folderButton;
    public final UIButton confirm;
    public final UIButton cancel;

    private final ContentType type;
    private final Consumer<String> callback;
    private String selectedFolder = "";

    public static String getTypeName(ContentType type)
    {
        if (type == ContentType.FILMS)
        {
            return "Film";
        }
        if (type == ContentType.MODELS)
        {
            return "Model";
        }
        if (type == ContentType.PARTICLES)
        {
            return "Particle Scheme";
        }
        String id = type.getId();
        if (id.isEmpty())
        {
            return id;
        }
        return Character.toUpperCase(id.charAt(0)) + id.substring(1);
    }

    public UICreateAssetOverlayPanel(ContentType type, Consumer<String> callback)
    {
        this(type, "", callback);
    }

    public UICreateAssetOverlayPanel(ContentType type, String defaultFolder, Consumer<String> callback)
    {
        super(IKey.constant("Create"));

        this.type = type;
        this.callback = callback;
        this.selectedFolder = defaultFolder != null ? defaultFolder : "";

        /* Consistent spacing: 12px outer margin, 26px row pitch, labels in a fixed 84px column. */
        int margin = 12;
        int labelW = 84;
        int fieldX = margin + labelW;
        int rowH = 20;
        int pitch = 26;

        /* Row 1: Type */
        this.typeLabel = new UILabel(IKey.constant("Type:"));
        this.typeLabel.relative(this.content).x(margin).y(margin).w(labelW).h(rowH);

        this.typeValue = new UILabel(IKey.constant(getTypeName(type)));
        this.typeValue.relative(this.content).x(fieldX).y(margin).w(1F, -fieldX - margin).h(rowH);

        /* Row 2: File Name */
        this.nameLabel = new UILabel(IKey.constant("File Name:"));
        this.nameLabel.relative(this.content).x(margin).y(margin + pitch).w(labelW).h(rowH);

        this.nameField = new UITextbox(120, (str) -> {});
        this.nameField.filename();
        this.nameField.relative(this.content).x(fieldX).y(margin + pitch).w(1F, -fieldX - margin).h(rowH);

        /* Row 3: Save Folder */
        this.folderLabel = new UILabel(IKey.constant("Save Folder:"));
        this.folderLabel.relative(this.content).x(margin).y(margin + pitch * 2).w(labelW).h(rowH);

        String btnLabel = this.selectedFolder.isEmpty() ? "(root)" : this.selectedFolder;
        UIButton folderButton = new UIButton(IKey.constant(btnLabel), (b) ->
        {
            UIRepositoryFolderPickerOverlayPanel picker = new UIRepositoryFolderPickerOverlayPanel(
                type,
                this.selectedFolder,
                (folder) ->
                {
                    this.selectedFolder = folder;
                    b.label = IKey.constant(folder.isEmpty() ? "(root)" : folder);
                }
            );
            UIOverlay.addOverlay(this.getContext(), picker, 520, 320);
        });
        this.folderButton = folderButton;
        this.folderButton.relative(this.content).x(fieldX).y(margin + pitch * 2).w(1F, -fieldX - margin).h(rowH);

        /* Row 4: Buttons (bottom-right, same 12px margin and 6px gap). */
        this.confirm = new UIButton(UIKeys.GENERAL_CONFIRM, (b) -> this.submit());
        this.confirm.relative(this.content).x(1F, -margin - 80 - 6 - 80).y(1F, -margin - rowH).w(80).h(rowH);

        this.cancel = new UIButton(UIKeys.ADDON_CANCEL, (b) -> this.close());
        this.cancel.relative(this.content).x(1F, -margin - 80).y(1F, -margin - rowH).w(80).h(rowH);

        this.content.add(this.typeLabel, this.typeValue, this.nameLabel, this.nameField, this.folderLabel, this.folderButton, this.confirm, this.cancel);
    }

    private void submit()
    {
        String name = this.nameField.getText().trim();
        if (name.isEmpty())
        {
            return;
        }

        this.close();

        if (this.callback != null)
        {
            this.callback.accept(this.selectedFolder + name);
        }
    }

    @Override
    protected void onAdd(UIElement parent)
    {
        super.onAdd(parent);
        this.nameField.textbox.moveCursorToEnd();
        parent.getContext().focus(this.nameField);
    }
}
