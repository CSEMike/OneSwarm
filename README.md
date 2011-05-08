
# OneSwarm

* [WWW](http://www.oneswarm.org/)
* [Forum](http://forum.oneswarm.org/)
* [Wiki](https://github.com/CSEMike/OneSwarm/wiki)

# Building

#### Mac OS X

1.  Install the [developer tools](http://developer.apple.com/xcode/)
2.  Install git, ant, and ant-contrib from [macports](http://www.macports.org/)

  *  `sudo port install git`
  *  `sudo port install apache-ant`
  *  `sudo port install ant-contrib`
    
3.  `git clone git@github.com:CSEMike/OneSwarm.git`
4.  `export ANT_OPTS="-Xmx256m"`
4.  `ant`
5.  `ant run`

#### Windows

1.  Download and install the [latest JDK](http://www.oracle.com/technetwork/java/javase/downloads/jre-6u25-download-346243.html)
2.  Download the [ant build tool](http://ant.apache.org/bindownload.cgi)
  * [Ant installation instructions](http://ant.apache.org/manual/install.html)
3.  Update your JAVA_HOME and ANT_HOME environment variables appropriately:

  *  `set ANT_HOME=c:\apache-ant` (wherever you put it)
  *  `set JAVA_HOME=c:\jdk1.6.0_25` (wherever you put it)
  *  `set PATH=%PATH%;%ANT_HOME%\bin`

4.  Install misc. ant dependencies
  *  `cd %ANT_HOME%`
  *  `ant -f fetch.xml -Ddest=system`

5.  Install [ant-contrib](http://ant-contrib.sourceforge.net/) (put the ant-contrib-1.0b3.jar in `%ANT_HOME%\lib`)

6.  `git clone git@github.com:CSEMike/OneSwarm.git`
7.  `ant`
8.  `ant run`


# Testing

Before running the full suite, ensure that the appengine community server is running. (The community server / CHT tests require this.)

`ant test`
