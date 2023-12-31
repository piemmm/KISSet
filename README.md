# KISSet

A terminal program for connecting to a TNC or modem that only support the KISS protocol (NinoTNC, TH-D74, etc)

### Installers/Packages

Releases are available for:<br />
<a href="https://github.com/piemmm/KISSet/releases/tag/windows-latest">Windows</a> (.msi file)
<br/><a href="https://github.com/piemmm/KISSet/releases/tag/ubuntu-latest">Linux</a> (.deb file)
<br/><a href="https://github.com/piemmm/KISSet/releases/tag/macos-latest">macOS</a> (.dmg file)
<br/><a href="https://github.com/piemmm/KISSet/releases/tag/rpi-arm64-latest">Raspberry Pi 64bit (arm64)</a> (.deb file), <a href="https://github.com/piemmm/KISSet/releases/tag/rpi-armv7l-latest">32bit armv7l</a> (.deb file)<br/>

> [!NOTE]
> Packages ending with 'GUI' are the full graphical version - the non-'GUI' packages are console only


### Building from source

KISSet uses Bellsofts liberica jdk which has java-fx bundled in. If you require to use a different jdk, you will need to install java-fx separately or add the dependencies to the pom.xml


### Next Planned features / Todo

* Remember remote connecting users colour choices for ANSI colour support
* Access control for the PMS system
* Finish Net/ROM support
* Multi window mode
* Flashing/notification icon/system tray icon for new PMS messages
* Interface statistics window
* Terminal support (for systems with no GUI)


![Picture of running node](./doc/screen2.png)

