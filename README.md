# 🧥 **Armament**

Adds the ability to create configurable custom armors using resource packs.

---

## 🎧 **Key Features**

| Feature | Description |
| --- | --- |
| **🛡️ Custom Armor Sets** | Easily add your own armor sets to Minecraft. |
| **⚡ Performance Optimization** | Built to ensure smooth server performance. |
| **💡 User-Friendly** | Simple commands for reloading configuration and giving armor to players. |
| **⚙ Configurable** | Manage your custom armors via a straightforward `armors.yml` file. |
| **🎨 Resource Pack Generation** | Automatically generate a resource pack from your configuration. |
| **🔃 Cross-Version Compatibility** | Supports Minecraft versions from `1.20` to the latest. |
| **🖼️ GUI Interface** | Browse and select custom armor sets through an intuitive GUI. |

---

## 💻 **Available Commands**

| Command | Description |
| --- | --- |
| **`/armament reload`** | Reload the plugin's configuration without a server restart. |
| **`/armament give <armor_name> <piece> [player]`** | Grant a custom armor piece to a player. |
| **`/armament giveset <armor_name> [player]`** | Grant a full custom armor set to a player. |
| **`/armament gui`** | Open the GUI to browse and select custom armor sets. |

---

## 🔒 **Permissions**

| Permission | Description |
| --- | --- |
| **`armament.admin`** | Grants access to all admin commands. |

---

## 📩 **Installation Steps**

1. **Download Armament:**
   - Get the latest version from [Modrinth](https://modrinth.com/plugin/armament).

2. **Install:**
   - Place the `.jar` file into your server's `plugins` directory.
   - Example: `/plugins/Armament-x.x.x.jar`

3. **Activate:**
   - Restart your server to load the plugin.

4. **Customize:**
   - Edit the `armors.yml` file located in `plugins/Armament/armors.yml` to add your custom armor sets.
   - Example configuration:
     ```yaml
     armors:
       epic:
         custom_model_data: 1
         helmet:
            name: "&6Epic Chestplate"
            lore:
            - "&7Custom Epic Lore!"
         chestplate:
            name: "&6Epic Chestplate"
            lore:
            - "&7Custom Epic Lore!"
         leggings:
            name: "&6Epic Leggings"
            lore:
            - "&7Custom Epic Lore!"
         boots:
            name: "&6Epic Boots"
            lore:
            - "&7Custom Epic Lore!"
     ```

5. **Add Resources:**
   - Place your custom armor textures in the `source_files` directory:
     - Armor layers: `source_files/layer_files/`
     - Armor display items: `source_files/item_files/`

6. **Load Resource Pack:**
   - Upload the generated resource pack to [mc-packs.net](https://mc-packs.net).
   - Update the `server.properties` file:
     | Property | Value |
     | --- | --- |
     | `resource-pack` | *URL to resource pack* |
     | `resource-pack-sha1` | *SHA1 hash of the resource pack* |
     | `require-resource-pack` | `true` |
   - **Note:** If you're using an existing resource pack, merge it with the Armament resource pack.

---

## 🎨 **Creating Custom Armor Textures**

To create custom armor textures for Armament:

1. **Open the Template:**
   - Locate the `armor_model.bbmodel` file in [src/main/resources](https://github.com/Furq07/Armament/tree/main/src/main/resources).
   - Open this file in BlockBench to get an understanding of the armor structure.

2. **Customize the Model:**
   - Ensure your model adheres to Minecraft's armor structure.

3. **Export Textures:**
   - Export your custom armor textures.

4. **Prepare Texture Files:**
   The plugin requires specific naming conventions for the texture files:

   a) **Layer Files** (Required):
      In `source_files/layer_files/`:
      - `armorName_layer_1.png`: For helmet, chestplate, and boots
      - `armorName_layer_2.png`: For leggings

   b) **Item Files** (Optional, based on armors.yml):
      In `source_files/item_files/`:
      - `armorName_helmet.png`
      - `armorName_chestplate.png`
      - `armorName_leggings.png`
      - `armorName_boots.png`

   Replace `armorName` with the name of your custom armor set as defined in `armors.yml`.

5. **Place Textures:**
   - Put the armor layer textures (`*_layer_1.png` and `*_layer_2.png`) in `source_files/layer_files/`.
   - Put the display item textures (e.g., `*_helmet.png`) in `source_files/item_files/`.

6. **Update Configuration:**
   - In `armors.yml`, reference your new textures using the appropriate armor name.
   - Ensure the armor name in the configuration matches the prefix used in your texture file names.

**Note:** The item files (helmet, chestplate, leggings, boots) are optional and depend on your `armors.yml` configuration. If you define a specific armor piece in the config, make sure to provide the corresponding item texture.

---

## 📞 **Support**

For assistance, visit the [GitHub Repository](https://github.com/furq07/armament/issues) or join our [Discord Server](https://discord.gg/7ugrBEKza4).

---

## 📜 **License**

Armament is released under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).

---

## 🤝 **Partner**

<p align="center"> <a href="https://billing.revivenode.com/aff.php?aff=517"> <img src="https://versions.revivenode.com/resources/banner_wide_one.gif" alt="Partner GIF"> </a> </p> <p align="center"> Use code <b>FURQ</b> for 15% off your order! </p>