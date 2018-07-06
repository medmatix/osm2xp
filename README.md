# osm2xp
This project is based on original OSM2XP by Benjamin Blanchet, code was originally taken from code.google.com/p/osm2xp
# Original osm2xp 
Supports generating *buildings* using facade set from OSM data loaded from pbf file, as well as generating
* Forest zones
* Some objects by rules 

I've needed custom generation for small terrain area around an airfield and decided to extend old OSM2XP app to support this.
Since initial task is generating X-Plane 10 scenery, most fixes I've done and all testing was done for X-Plane 10 scenery generation. I can't guarantee other modes will work correctly, but if you want to post some issues or offer some ideas/help for them - feel free to contact me.

## Already done

### Generation of
* Roads, with trying to select best matching road type, if lane count data is present in OSM
* Railways
* Power lines
* Barriers, two types are supported - fence and wall
* Water tanks, fuel tanks, gasometers - using special facade
* Chimneys - are generated by inserting special objects taken from opensceneryx and ruscenery packages, as well as created by myself. Object with height closest to necessary is selected, e.g. for chimney with height 80m object with height=75m will be chosen
* Bridges for roads and railways based on OSM markup
* For buildings with type *"garage"* special facade is used, since using regular building facade for garage usually gives poor result

### As well as
* Using _building:levels_ tag value if no _height_ tag specified for building
* Improved facade set editor - added facade preview, ability to delete facade and specify facades for fence or wall 
* Generating smaller area, then 1 tile - just as much as OSM PBF file defines. OSM PBF file can be obtained e.g. using [bbbike.org](https://extract.bbbike.org/ "bbbike.org")
* Migration to Java 8, using some newer libraries and some UI fixes
* Basic OSM multipolygon support

## Installation
You need at least Java 8 JRE or JDK to be installed

Download program archive from [SourceForge](https://sourceforge.net/projects/osm2xp/ "SourceForge") and unpack to any folder. It's better to use path without spaces. Launch program using osm2xp/osm2xp.exe executable.

For now program is available for Windows only. If you need it for other OS, please create an issue on GitHub.

## Usage

Please refer [Quick Start Guide](https://github.com/32kda/osm2xp/wiki/Quick-Start "Guide") 

## Samples of generated scenarios

![Screenshot 1](https://32kda.github.io/osm2xp/screenshots/CH750_4.png "Screenshot 1")

![Screenshot 2](https://32kda.github.io/osm2xp/screenshots/CH750_2.png "Screenshot 2")

## Participate
You can help with coding, if you know Java/Eclipse, with creating better default facade set (current has a lot of problems) or just filing issues and posting improvement suggestions. Any help is welcome. 

## Nearest plans

* Generate cooling towers - using objects, like it's done for chimneys
* Multipolygon (e.g. poly with holes) support for forests & buildings - partially done
* More options in the GUI
* Make more facade categories - add garage and tank/gasometer facade types
