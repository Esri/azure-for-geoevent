# Microsoft Azure Connectors

**This item has been deprecated. Please refer to the blog article [here](https://community.esri.com/t5/arcgis-geoevent-server-blog/working-with-azure-event-hubs-using-kafka/ba-p/1235366) for an alternative method for Azure Event Hubs integration. Please consider contributing an idea to the [Esri Community](https://community.esri.com/t5/arcgis-geoevent-server-ideas/idb-p/arcgis-geoevent-server-ideas) if you need similar functionality.**

ArcGIS GeoEvent Server sample Azure connectors for connecting to Microsoft Azure Hubs and Devices.

![App](azure-for-geoevent.png?raw=true)

## Features
* Azure Transport and Connectors

## Instructions

Building the source code:

1. Make sure Maven and ArcGIS GeoEvent Server SDK are installed on your machine.
2. Run 'mvn install -Dcontact.address=[YourContactEmailAddress]'

Installing the built jar files:

1. Copy the *.jar files under the 'target' sub-folder(s) into the [ArcGIS-GeoEvent-Server-Install-Directory]/deploy folder.

## Requirements

* ArcGIS GeoEvent Server (Certified with version 10.6.x).
* ArcGIS GeoEvent Server SDK.
* Java JDK 1.8 or greater.
* Maven.

## Resources

* [ArcGIS GeoEvent Gallery](http://links.esri.com/geoevent-gallery) 
* [ArcGIS GeoEvent Server Resources](http://links.esri.com/geoevent)
* [ArcGIS Blog](http://blogs.esri.com/esri/arcgis/)
* [twitter@esri](http://twitter.com/esri)

## Issues

Find a bug or want to request a new feature?  Please let us know by submitting an issue.

## Contributing

Esri welcomes contributions from anyone and everyone. Please see our [guidelines for contributing](https://github.com/esri/contributing).

## Licensing
Copyright 2013 Esri

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

A copy of the license is available in the repository's [license.txt](license.txt?raw=true) file.
