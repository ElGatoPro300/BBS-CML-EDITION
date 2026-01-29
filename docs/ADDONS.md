# Creating Addons for BBS Mod

This guide explains how to create addons for the BBS Mod. The addon system allows you to extend the mod's functionality by adding new forms, clips, dashboard panels, and more, without needing to modify the core mod code or use complex Mixins.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Project Setup](#project-setup)
3. [Addon Structure](#addon-structure)
   - [BBSAddon (Common/Server)](#bbsaddon-commonserver)
   - [BBSClientAddon (Client)](#bbsclientaddon-client)
4. [Utility Classes](#utility-classes)
5. [Step-by-Step Example](#step-by-step-example)

## Prerequisites

- Basic knowledge of Java and Fabric modding.
- A Fabric development environment set up.

## Project Setup

### fabric.mod.json

To register your addon, you need to add specific entrypoints to your `fabric.mod.json`.

- `bbs-addon`: For your common/server-side addon class.
- `bbs-addon-client`: For your client-side addon class.

```json
{
  "schemaVersion": 1,
  "id": "my_bbs_addon",
  "version": "1.0.0",
  "name": "My BBS Addon",
  "description": "An awesome addon for BBS Mod",
  "authors": [
    "Your Name"
  ],
  "contact": {
    "homepage": "https://example.com",
    "sources": "https://github.com/your/repo"
  },
  "license": "MIT",
  "icon": "assets/my_bbs_addon/icon.png",
  "environment": "*",
  "entrypoints": {
    "main": [
      "com.example.addon.MyAddon"
    ],
    "client": [
      "com.example.addon.client.MyAddonClient"
    ],
    "bbs-addon": [
      "com.example.addon.MyBBSAddon"
    ],
    "bbs-addon-client": [
      "com.example.addon.client.MyBBSClientAddon"
    ]
  },
  "depends": {
    "fabricloader": ">=0.14.21",
    "minecraft": "~1.20.1",
    "java": ">=17",
    "bbs_mod": ">=1.0.0"
  }
}
```

## Addon Structure

The BBS Mod addon system is divided into two main parts: the common/server side and the client side.

### BBSAddon (Common/Server)

Extend the `mchorse.bbs_mod.addons.BBSAddon` class to register content that exists on both the server and client, such as:

- **Forms**: New actor models or shapes.
- **Camera Clips**: New types of camera movements.
- **Action Clips**: New types of actions in films.
- **Settings**: Global mod settings.
- **Source Packs**: Resource packs.

**Example:**

```java
package com.example.addon;

import mchorse.bbs_mod.addons.BBSAddon;
import mchorse.bbs_mod.events.register.RegisterFormsEvent;
import mchorse.bbs_mod.events.register.RegisterCameraClipsEvent;

public class MyBBSAddon extends BBSAddon
{
    @Override
    protected void registerForms(RegisterFormsEvent event)
    {
        // Register a new form
        // event.getForms().register("my_form", MyForm.class);
    }

    @Override
    protected void registerCameraClips(RegisterCameraClipsEvent event)
    {
        // Register a new camera clip
        // event.getFactory().register(Link.create("my_addon", "custom_clip"), MyClip.class, new ClipFactoryData(Icons.FILM, 0xFF0000));
    }
}
```

### BBSClientAddon (Client)

Extend the `mchorse.bbs_mod.addons.BBSClientAddon` class to register client-only content, such as:

- **Dashboard Panels**: New tabs in the main dashboard.
- **Form Categories**: Categories for the form picker.
- **Client Settings**: Visual-only settings.
- **Localization (L10n)**: Translation files.

**Example:**

```java
package com.example.addon.client;

import mchorse.bbs_mod.addons.BBSClientAddon;
import mchorse.bbs_mod.events.register.RegisterDashboardPanelsEvent;
import mchorse.bbs_mod.events.register.RegisterL10nEvent;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;

public class MyBBSClientAddon extends BBSClientAddon
{
    @Override
    protected void registerDashboardPanels(RegisterDashboardPanelsEvent event)
    {
        // Register a new dashboard panel
        // UIDashboard dashboard = event.getDashboard();
        // dashboard.addPanel(new MyCustomPanel(dashboard));
    }

    @Override
    protected void registerL10n(RegisterL10nEvent event)
    {
        // Register translation files
        // event.getL10n().register((lang) -> Link.create("my_addon", "strings/" + lang + ".json"));
    }
}
```

## Utility Classes

To make development easier, BBS Mod provides two utility classes that give you direct access to core components without complex dependency injection.

### `mchorse.bbs_mod.BBS` (Common)

Use this class to access:
- `BBS.getEvents()`: The global event bus.
- `BBS.getForms()`: The form architect.
- `BBS.getFactoryCameraClips()`: The camera clip factory.
- `BBS.getGameFolder()`, `BBS.getAssetsFolder()`: Important directories.

### `mchorse.bbs_mod.BBSClient` (Client)

Use this class to access:
- `BBSClient.getDashboard()`: The main UI dashboard.
- `BBSClient.getTextures()`: Texture manager.
- `BBSClient.getSounds()`: Sound manager.
- `BBSClient.getCameraController()`: The active camera controller.

## Step-by-Step Example

Here is a simple example of an addon that adds a "Hello World" message to the console when initialized.

**1. Create the Common Addon Class**

```java
package com.example.addon;

import mchorse.bbs_mod.addons.BBSAddon;

public class ExampleAddon extends BBSAddon
{
    // You can override methods here to register content
}
```

**2. Create the Client Addon Class**

```java
package com.example.addon.client;

import mchorse.bbs_mod.addons.BBSClientAddon;

public class ExampleClientAddon extends BBSClientAddon
{
    // You can override methods here to register UI components
}
```

**3. Register in `fabric.mod.json`**

Ensure your `fabric.mod.json` contains the entrypoints pointing to these classes as shown in the [Project Setup](#project-setup) section.

That's it! Your addon is now hooked into the BBS Mod system.
