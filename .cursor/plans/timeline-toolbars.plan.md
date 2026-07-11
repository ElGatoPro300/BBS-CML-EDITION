# Timeline Toolbars — Plan de implementación

> Branch de trabajo: **`toolbars`**
> Enfoque: **aditivo** — no modificar menús contextuales ni keybinds existentes (salvo fixes acordados en undo/redo embebida).

---

## Objetivo

Añadir toolbars estilo Blender en las tres timelines del film editor:

| Timeline | Componente base | Toolbar |
|----------|-----------------|---------|
| Camera | `UIClipsPanel` + `UIClips` (factory cámara) | Clips |
| Replay | `UIReplaysEditor` + `UIKeyframes` | Keyframes/Tracks |
| Action | `UIClipsPanel` + `UIClips` (factory acciones) | Clips (action) |

**Contextual:** una sola toolbar visible. Al entrar en `embedView(UIKeyframeEditor)` dentro de camera/action → swap a toolbar Keyframes (sin apartado Actor). Al salir → toolbar de clips.

---

## Reglas globales de UX

### Toolbar raíz
- Apartados principales: **solo iconos** (`Icons` existentes; repetición OK).
- Hover → tooltip con nombre del apartado (`IKey` / `UIKeys`).
- Orden clips: `[Transporte] [Añadir] [Edición] [Selección] [Transformar] [Historial]`
- Orden replay: `[Transporte] [Añadir] [Edición] [Selección] [Keyframes/Tracks] [Historial] [Actor]`

### Submenús
- **Siempre** texto/label de la operación.
- Icono del menú contextual **si existe**, a la izquierda; **slot fijo** para alinear textos (vacío si no hay icono).
- Atajo de teclado alineado a la derecha (desde `KeyCombo` si existe).
- Submenú anidado: flecha `>` (o `<` / `^` según dirección).
- **Todas** las opciones visibles aunque deshabilitadas.

### Popups
- Render en **overlay global** (pantalla/dashboard), **no** recortados al panel del timeline.
- Apartados raíz: desplegar hacia **arriba** por defecto (toolbar abajo).
- Submenús: hacia **derecha** por defecto; invertir si no hay espacio.
- Hover abre submenús anidados; cierre si ratón se aleja **X px** de cualquier rect abierto (`TimelineToolbarSettings.TOOLBAR_MENU_DISMISS_DISTANCE_PX`).

### Estados deshabilitados
| Tipo | Visual |
|------|--------|
| Normal | Texto + icono atenuados (gris) |
| Causa externa (viewport oculto) | Atenuado + **tooltip en rojo** con razón |
| Eliminar (activo) | Icono + **línea roja vertical** (como context menu) |

### Modos interacción (🎯) — Fase 3
- Añadir clip, Loop In/Out, Pegar, Insertar keyframe, Actor viewport…
- Outline pulsante en tick bajo cursor (opacidad 100% ↔ 0%).
- Click confirma; **Esc** o **clic derecho** cancela **sin** menú contextual.
- Cancelar al cerrar embebida o cambiar toolbar.

### Constantes (`TimelineToolbarSettings.java`)
- `TOOLBAR_HEIGHT = 32`
- `TOOLBAR_MENU_DISMISS_DISTANCE_PX` (ajustable)
- `TOOLBAR_ICON_SLOT_WIDTH`
- etc.

---

## Jerarquía de apartados

### Clips (Camera / Action)

**Transporte:** Play/Pause, prev/next tick, prev/next clip, jump, cycle editor.

**Añadir:**
- Modo colocación (cursor ratón / tick reproducción / encima seleccionado)
- Camera: tipos por pestañas (Camera, Resource, Screen, Anchor, Extras) + import replay + micrófono (solo camera)
- Action: Bloques & mundo, Items, Combate, Otros

**Edición:** Copy, Cut, Paste, Presets, **Eliminar**, **Ver todos / reset zoom**, reorganize (camera).

**Selección:** Deselect, select before/after, toggle enable.

**Transformar:** Cut at cursor, shift to cursor, shift duration, fade in/out.

**Historial:** Undo, Redo.

### Keyframes (Replay / embebida)

**Transporte / Historial / Modos:** igual que arriba (heredado UIFilmPanel).

**Añadir:** Insertar keyframe (modo 🎯).

**Edición:** clipboard keyframes, delete, reset zoom, interpolación, spread, scale time, stack, adjust values, round, select all, etc.

**Selección:** select left/right, prev/next keyframe, select same.

**Keyframes/Tracks:**
- Filter tracks… (`UIKeyframeSheetFilterOverlayPanel`)
- Rename track (`UIRenameSheetOverlayPanel`)
- Animation to pose keyframes…
- Pose to limbs
- Edit track / Exit track

**Actor** (solo replay, no embebida en clip):
- Add replay here, Move actor here
- Deshabilitado si visor 3D no visible (Ventana → Visor desmarcado o layout oculto)

---

## Fases de implementación

### Fase 1 — Shell UI (sin invocar operaciones)
- Paquete `ui/film/toolbar/`
- `TimelineToolbar`, `ToolbarMenu`, `ToolbarMenuItem`, overlay popups
- Registro mock con jerarquía completa
- Montaje aditivo en `UIClipsPanel`, `UIReplaysEditor`
- Listener `embedView` → swap toolbar
- **No:** modos 🎯, wire handlers, undo fix

### Fase 2 — Conectar operaciones
- Registro real reutilizando handlers de keybinds/context
- `.active()` / condiciones habilitado
- Actor + detección viewport

### Fase 3 — Modos interacción
- `UIInteractionModeOverlay`
- Outline pulsante, Esc / RMB cancel

### Fase 4 — Undo/redo + embebida
- Enfoque B simétrico: undo cierra embebida si afecta fuera; **redo reabre** embebida
- Restaurar scroll/zoom/viewport en undo data
- Cambio acotado en `UIFilmUndoHandler` / collectUndoData de paneles

---

## Restricciones

- **No tocar** menús contextuales existentes.
- **No tocar** keybinds (F9, etc.).
- **No incluir** Film Controller (tiene su propia toolbar en preview).
- Localización vía `UIKeys` → `en_us.json`.
- Estilo código: `CONTRIBUTING.md` (llaves nueva línea, `this.`, sufijos `F`/`D`/`L`).
- **No modificar Gradle.**

---

## Archivos clave existentes

| Archivo | Rol |
|---------|-----|
| `UIClips.java` | Timeline clips, keybinds, embedView, addPreview |
| `UIClipsPanel.java` | Panel camera/action |
| `UIReplaysEditor.java` | Replay timeline, context tracks |
| `UIKeyframes.java` | Editor keyframes |
| `UIFilmPanel.java` | Transporte, looping, undo, cameraEditor setup |
| `Keys.java` | KeyCombo definitions |
| `UIContext.java` | toggleKeybinds, overlay |

---

## Testing

- `./gradlew runClient`
- Fase 1: verificar toolbar visible, menús abren/cierran, swap embebida, sin acciones reales
- Fase 2+: probar operaciones vs keybinds/context menu

---

## Pendiente futuro (fuera de scope inicial)

- Segunda textura PNG para iconos toolbar
- Toolbar reposicionable
- Densidad compacta/ampliada UI
- Constantes en menú de ajustes del mod
