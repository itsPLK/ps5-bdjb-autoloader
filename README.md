<p align="center">
 <img src="./ps5loader.png" width="128" />
</p>
<h1 align="center">PS5 BD-JB Autoloader</h1>
<h3 align="center">Fork of <a href="https://github.com/Gezine/BD-JB5">BD-JB5</a></h3>
&nbsp;
<p align="center">Automatically loads the kernel exploit and your elf payloads.<br>Supports PS5 firmwares 4.03-12.00.</p>

<p align="center">
    <b>Other Autoloaders:</b><br>
    <a href="https://github.com/itsPLK/ps5-y2jb-autoloader">Y2JB</a> | 
    <a href="https://github.com/itsPLK/ps5-lua-autoloader">Lua</a>
</p>

<p align="center">
 <img src="./bdjb_screenshot.png" width="600" />
</p>


## How to Use

There are two ways to use the autoloader:

### 🟢 Option 1: Payload Manager

If no `autoload.txt` config is found, the autoloader will automatically launch **[Payload Manager](https://github.com/itsPLK/ps5-payload-manager)** — a fully-featured PS5 payload manager with a web UI. This lets you configure and send payloads directly from your browser, without needing to manually set up config files or transfer ELF files ahead of time.

Just run the autoloader — if there's nothing configured, Payload Manager starts automatically.

> **Note:** Payload Manager also has its own built-in autoload feature, which lets you configure payloads to load automatically on startup — all managed through its web UI. This is separate from the `autoload.txt` mechanism described below.

---

### ⚙️ Option 2: Manual Config (`autoload.txt`)

For a fixed, automated payload chain, you can configure payloads manually:

- Create a directory named `ps5_autoloader`.
- Inside this directory, place your `.elf` / `.bin` files, and an `autoload.txt` file.
  - In `autoload.txt`, list the files you want to load, one filename per line.
  - Filenames are case-sensitive — ensure each name exactly matches the file.
  - You can add lines like `!1000` to make the loader wait 1000 ms before sending the next payload.
- Put the `ps5_autoloader` directory in one of these locations (priority order - highest first):
  - Root of a USB drive
  - Internal drive: `/data/ps5_autoloader`

> **Note:** When an `autoload.txt` config is found, Payload Manager is **not** launched automatically. If you also want Payload Manager available, place `pldmgr.elf` in your `ps5_autoloader` directory and add it to `autoload.txt`.

## Setup Instructions

This autoloader is deployed via a BD-R disc.

1. Download the **PS5 BD-JB Autoloader ISO** from the [Releases](https://github.com/itsPLK/ps5-bdjb-autoloader/releases) page.
2. Burn the ISO to a BD-R(E) disc using software like `ImgBurn` (use UDF 2.50 filesystem).
3. Insert the disc into the PS5 and launch it from the "Media" tab.

*Note: Since this is a disc-based loader, updates to the loader itself require burning a new ISO. However, your payloads on USB or internal storage can be updated at any time.*


## Additional Info

<Details>
<Summary><i>How to use custom ELF Loader version?</i></Summary>

By default, the autoloader uses a custom version of **elfldr** that only accepts connections from the PS5 itself (localhost). This improves security by preventing other devices on your network from sending payloads to your console.

If you want to use a "normal" ELF Loader that allows sending payloads from any device:
1. Place your custom ELF Loader (e.g. `elfldr.elf`) in the `ps5_autoloader` directory.
2. Add `elfldr.elf` to your `autoload.txt`.
3. **Note**: If you are loading other payloads right after `elfldr.elf` in your `autoload.txt`, add a sleep command immediately after it (like `!4000` to sleep for 4 seconds) to give the new ELF Loader time to start up and listen before subsequent payloads are sent.

Example `autoload.txt`:
```text
# Load custom ELF Loader
elfldr.elf
# Give it 4 seconds to start up (only needed if sending more payloads)
!4000
# Send other payloads
ftpsrv.elf
```
</Details>

## Credits

* **[Gezine](https://github.com/Gezine)** - [BD-JB5](https://github.com/Gezine/BD-JB5), [Poops exploit implementation](https://github.com/Gezine/BD-JB5/blob/main/payloads/poops/src/org/bdj/external/Poops.java)
* **[TheFlow](https://github.com/theofficialflow)** — BD-JB documentation & native code execution sources.  
* **[hammer-83](https://github.com/hammer-83)** — PS5 Remote JAR Loader reference.  
* **[john-tornblom](https://github.com/john-tornblom)** — [BDJ-SDK](https://github.com/john-tornblom/bdj-sdk) and [ps5-payload-sdk](https://github.com/ps5-payload-dev/sdk/) used for compilation.  
* **[ufm42](https://github.com/ufm42)** - [kexp](https://github.com/ufm42/kexp) used for PS5 post JB all-in-one shellcode
* **[kuba--](https://github.com/kuba--)** — [zip](https://github.com/kuba--/zip) used for bdj_unpatch elf payload.  

## Disclaimer

This tool is provided as-is for research and development purposes only. Use at your own risk. The developers are not responsible for any damage, data loss, or consequences resulting from the use of this software.

## License

This project is licensed under the GPL-3.0 License.

The original base code remains under its original MIT License (see LICENSE-MIT).
All unique modifications and additions in this project are licensed under GPL-3.0.

## Donate
- [donate to PLK](DONATE.md)
