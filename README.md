# Dataplugin
A plugin-based data integration solution for heterogeneous datasets.

## Requirements
 
* Java Development Kit (JDK) SE 1.8.0

## Installation

To compile the project use `jar` tool

```
[root@server ~]# git clone https://github.com/hydromex/dataplugin.git
[root@server ~]# jar -cvf dataplugin.war dataplugin-master/
```

## Adding a new dataset

To integrate a new dataset you should create a new plugin with the purpose of extracting, transforming and loading the dataset.
Each plugin should follow the next proposed steps:

 * Gathering data requirements - Establish the requirements that the data must meet when stored.
 * Analyzing data sets - Analyze the structure and format of the data set to understand how they are compose
   and how it is possible to store them
 * Design plugin - Design the plugin structure to be implemented to load the data set. It consists of genereating class or sequence
   diagrams or all necessary representations needed to impement the desired functionality.
 * Design data schema - Create a data model to use when storing data set.
 * Implement schema - Create the schema in the database manager system selected for the database.
 * Develop plugin - Code the plugin.
 * Direct load tests - Test loading the plugin result directly into the database.
 * Integration tests - Test the interaction of PluginManager with the plugin.
 * Plugin tests - Test plugin execution and loadint into database.
 * Validate plugin - Confirm compliance withe the requirements.
 * Execute plugin - Feel free to execute plugin

 
## License

This project is licensed under the GNU v3 License - see the [LICENSE](LICENSE) file for details

## Acknowledgments

**Note: Documents are in spanish.**

* [Development of a software architecture for the download, geospatial aggregation and query of data from meteorological stations](https://docs.google.com/document/d/1ZUPZamiJFUSlATse8HChCSw4FZuQfgOItGcq17LQW8A/edit?usp=sharing)

## Contributors

* [jdosornio](https://github.com/jdosornio)
* [Herrera93](https://github.com/herrera93)
