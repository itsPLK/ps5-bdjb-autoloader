<p align="center">
 <img src="./ps5loader.png" width="128" />
</p>
<h1 align="center">PS5 BD-JB Autoloader</h1>
<h3 align="center">Fork of <a href="https://github.com/hammer-83/ps5-jar-loader">hammer-83's ps5-jar-loader</a></h3>
&nbsp;
<p align="center">Automatically loads your .elf, .bin and .jar payloads.<br>Supports PS5 firmwares up to 7.61.</p>

<p align="center">
    <b>Other Autoloaders:</b><br>
    <a href="https://github.com/itsPLK/ps5-y2jb-autoloader">PS5 Y2JB Autoloader</a> | 
    <a href="https://github.com/itsPLK/ps5-lua-autoloader">PS5 Lua Autoloader</a>
</p>

<p align="center">
 <img src="./bdjb_screenshot.png" width="600" />
</p>


## How to Use

- Create a directory named `ps5_autoloader`.
- Inside this directory, place your `.elf`, `.bin`, and `.jar` files, and an `autoload.txt` file.
  - In `autoload.txt`, list the files you want to load, one filename per line.
  - Filenames are case-sensitive — ensure each name exactly matches the file.
  - You can add lines like `!1000` to make the loader wait 1000 ms before sending the next payload.
  - You can add lines like `@ message` to show a notification on the PS5.
  - Do NOT include `elfldr.elf` in `autoload.txt` unless you want to override the bundled version; it is loaded automatically when needed for ELF/BIN payloads.
- Put the `ps5_autoloader` directory in one of these locations (priority order - highest first):
  - Root of a USB drive
  - Internal drive: `/data/ps5_autoloader`
  - Root of the disc itself.


## Setup Instructions

This autoloader is deployed via a BD-R disc.

1. Download the **PS5 BD-JB Autoloader ISO** from the [Releases](https://github.com/itsPLK/ps5-bdjb-autoloader/releases) page.
2. Burn the ISO to a BD-R(E) disc using software like `ImgBurn` (use UDF 2.50 filesystem).
3. Insert the disc into the PS5 and launch it from the "Media" tab.

*Note: Since this is a disc-based loader, updates to the loader itself require burning a new ISO. However, your payloads on USB or internal storage can be updated at any time.*


## Additional Info

<Details>
<Summary><i>How to use custom ELF Loader version?</i></Summary>

By default, the autoloader uses a bundled version of **elfldr** that only accepts connections from the PS5 itself (localhost). This improves security by preventing other devices on your network from sending payloads to your console.

If you want to use a "normal" ELF Loader that allows sending payloads from any device:
1. Place your `elfldr.elf` in the `ps5_autoloader` directory.
2. Add `elfldr.elf` as the first entry in your `autoload.txt`.
</Details>

<Details>
<Summary><i>Payload Manager integration</i></Summary>

The autoloader includes **Payload Manager**. Using it is the most reliable way to load etaHEN/kstuff, as it closes the Disc app before sending the payloads. To use it, make `pldmgr.elf` the **only** item in your `autoload.txt`.
</Details>


## Credits

There are so many who decided to share the knowledge with the community to make this project possible.
- [Andy "theflow" Nguyen](https://github.com/theofficialflow) for discovering and sharing BD-J vulnerabilities and native execution techniques without which none of the work in this repo would be possible.
- Specter for his Webkit implementations of PS5 kernel access which served as a base for Java implementation: [IPV6](https://github.com/Cryptogenic/PS5-IPV6-Kernel-Exploit), [UMTX](https://github.com/PS5Dev/PS5-UMTX-Jailbreak/) and [Byepervisor](https://github.com/PS5Dev/Byepervisor).
- [Flat_z](https://github.com/flatz) for pretty much everything of significance that happened in PlayStation scene since as far back as PS3, including the UMTX exploitation strategy and AGC-based kernel r/w contained in this repo.
- [Cheburek3000](https://github.com/cheburek3000) for contributing an alternative implementation of UMTX exploitation. 
- [bigboss](https://github.com/psxdev) and [John Törnblom](https://github.com/john-tornblom) for their work specifically in BD-J area.
- [iakdev](https://github.com/iakdev) for the contribution of the menu loader.
- CryoNumb for ideas, code contributions and helping with testing on various firmware versions.
- All the other contributors to Specter's Webkit implementations: [ChendoChap](https://github.com/ChendoChap), [Znullptr](https://twitter.com/Znullptr), [sleirsgoevy](https://twitter.com/sleirsgoevy), [zecoxao](https://twitter.com/notnotzecoxao), [SocracticBliss](https://twitter.com/SocraticBliss), SlidyBat, [idlesauce](https://github.com/idlesauce), [fail0verflow](https://fail0verflow.com/blog/) [kiwidog](https://kiwidog.me/), [EchoStretch](https://github.com/EchoStretch), [LM](https://github.com/LightningMods).
- Testers of various firmware revisions: jamdoogen, Twan322, MSZ_MGS, KingMaxLeo, RdSklld, Ishaan, Kirua, PLK, benja44_, MisaAmane, Unai G, Leo.

## Development

*For technical details and building from source, see [DEVELOPMENT.md](DEVELOPMENT.md).*

## Disclaimer

This tool is provided as-is for research and development purposes only. Use at your own risk. The developers are not responsible for any damage, data loss, or consequences resulting from the use of this software.

## License

This project is licensed under the **GPL-3.0 License**.

The original project by hammer-83 did not include an explicit LICENSE file.  
All unique modifications and additions in this fork are licensed under **GPL-3.0**.

## Donate
- [donate to PLK](DONATE.md)
