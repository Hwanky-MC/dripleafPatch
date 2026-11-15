# DripleafPatch

![Java](https://img.shields.io/badge/Java-21-orange)
![Platform](https://img.shields.io/badge/Platform-Paper%20%7C%20Purpur%20%7C%20Leaf%201.21%2B-00b4d8)
![License](https://img.shields.io/badge/License-MIT-00ff88)

A lightweight, event-driven security plugin for **Paper**, **Purpur**, and **Leaf** (`1.21+`) that prevents the **Small Dripleaf duplication glitch** and enforces **multi-layer Shulker Box anti-nesting**, DripleafPatch protects your economy from nested Shulker exploits, purges existing corrupted containers, and notifies staff in real time.

---

## Commands & Permissions

Aliases: `/dlp`, `/dlpatch`, `/dripleaf`

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/dripleafpatch panel` | Open the interactive module control GUI | `dripleafpatch.panel` |
| `/dripleafpatch toggle <module> [on\|off]` | Toggle individual protection modules in real time | `dripleafpatch.toggle` |
| `/dripleafpatch set <setting> <value>` | Change `cleanup-mode` or `purge-action` modes | `dripleafpatch.set` |
| `/dripleafpatch status` | Display current status of all protection layers | `dripleafpatch.status` |
| `/dripleafpatch reload` | Reload `config.yml` | `dripleafpatch.reload` |

- **`dripleafpatch.admin`** *(default: `op`)* — Grants full access to all commands and the control panel.
- **`dripleafpatch.notify`** *(default: `op`)* — Receives real-time chat and sound alerts when an exploit attempt is blocked.

---
