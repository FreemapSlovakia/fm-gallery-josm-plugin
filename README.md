# Freemap.sk Gallery JOSM Plugin

Quickly hacked JSOM plugin to show pictures from freemap.sk.

## Building

First modify paths in [build.xml](./build.xml) file (`/home/martin/...`) and then run

```sh
ant jar
```

## Usage

After the plugin is compiled to the correct place then run JOSM and enable it in _Edit / Preferences / Plugins_; search for _fm-gallery_. Activate the layer with _Imagery / Freemap Gallery_. Images will be visible from the zoom 14 and will be loaded dynamically on map view change.

## Known issues

This is a quick and dirty implementation. If you get exception when using this plugin then just suppress it for the session.
