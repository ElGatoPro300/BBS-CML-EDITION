package mchorse.bbs_mod.ui.utils.gizmo;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.utils.Gizmo;
import mchorse.bbs_mod.ui.utils.StencilFormFramebuffer;

/**
 * Reusable pick -> hover -> drag lifecycle for a single {@link GizmoSurface}, replacing the
 * near-identical "check stencil pick, verify handle range, start drag" block that used to be
 * hand-duplicated in every editor. One instance per surface/editor; the underlying {@link Gizmo}
 * stays a singleton (mode, active handle, deferred render queue are shared across all editors,
 * exactly as before this refactor).
 */
public class GizmoController
{
    private final GizmoSurface surface;

    public GizmoController(GizmoSurface surface)
    {
        this.surface = surface;
    }

    /** Attempts to start a gizmo handle drag from whatever the surface's stencil last picked.
     *  Returns false (without side effects) if nothing was picked, or the pick wasn't a handle,
     *  so the caller can fall through to its normal (bone/form) pick handling. */
    public boolean tryStartHandleDrag(UIContext context, UIPropTransform transform)
    {
        if (transform == null)
        {
            return false;
        }

        StencilFormFramebuffer stencil = this.surface.getGizmoStencil();

        if (!stencil.hasPicked())
        {
            return false;
        }

        int index = stencil.getIndex();

        if (!Gizmo.isHandleIndex(index))
        {
            return false;
        }

        this.surface.prepareGizmoDrag(transform);

        return Gizmo.INSTANCE.start(index, context.mouseX, context.mouseY, transform);
    }

    /** Feeds the current stencil pick into the gizmo's continuous hover highlight. Safe to call
     *  every frame regardless of whether a drag is active; call from the surface's render pass
     *  right after its stencil pick is resolved for the frame. */
    public void updateHover()
    {
        StencilFormFramebuffer stencil = this.surface.getGizmoStencil();
        int index = stencil.hasPicked() ? stencil.getIndex() : -1;

        Gizmo.INSTANCE.setHoveredIndex(Gizmo.isHandleIndex(index) ? index : -1);
    }

    public void stop()
    {
        Gizmo.INSTANCE.stop();
    }
}
