# nifi_templates
Nifi is an open source data flow tool developed by the apache community for interfacing with Apache Hadoop and a range of other data platforms. Following DRY principles it allows users to create and store templates as a way of allowing code portability. This is a collection of useful templates for any hadoop or SQL user.

## Importing a Template
Go to main nifi screen and click the small blueprint icon in the top right corner as illustrated below:

![alt_text](resources/import_template_1.png)

Click browse and naviate to any file under this repo and open the XML file.

### Note:
Nifi will automatically create a copy of the xml file and store it as a .template file in `/conf/templates` under your nifi installation directory.
