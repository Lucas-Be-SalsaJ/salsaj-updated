SalsaJ
======

SalsaJ is available under the following licence:
http://creativecommons.org/licenses/by-nc-nd/3.0/legalcode


##Directories

- Jama = library for matrix computations, used in Salsaj ?
- ij = core ImageJ
- nom = library for reading FITS format
- images=icons
- skyview=sky coordinates for stars when using FITS images

##Dev Help

Commentary have been added through SalsaJ code to describe modifications and their goals:

* Adding code *(ex: add functionalities)*
    * EU_HOU ADD
    * EU_HOU ADD END
* Changing code *(ex: modify existing functionalities)*
    * EU_HOU CHANGES
    * EU_HOU CHANGES END
* Translation related *(IJ.getBundle() and IJ.getColorBundle())*
    * EU_HOU Bundle
    * EU_HOU Bundle =x *(ex: the x next line will contain code that permit translation)*
    * EU_HOU MISSING Bundle
* Translation related *(additional code that permit the translation (different from IJ.getBundle() and IJ.getColorBundle())*
    * EU_HOU Bundle ADD
    * EU_HOU Bundle ADD END

Note: Toolbars haven't really been updated, but they work kind of separately so it shouldn't be a problem. A lot of functionalities need to be tested (for instance, functionalities affiliated to Plot.java).
