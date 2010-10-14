<?php
	$latestVersion = 3052;

	if(! isset($platform) || $platform == '')
		$platform 	= @$_GET['platform'];
		
	if(! isset($platform) || $platform == '')
		exit();
		
	if(! isset($version) || $version == '')
		$version 	= @$_GET['version'];	
		
	if(! isset($version))
		$version 	= '';	
	
	if(substr($version,0, 7) == "2.1.0.1") {
	
	echo("3058\n");
	
	if($platform == "win32") { ?>
http://download2.eclipse.org/downloads/drops/S-3.0RC2-200406111814/eclipse-SDK-3.0RC2-win32.zip
<?php } else if($platform == "motif") { ?>
http://download2.eclipse.org/downloads/drops/S-3.0RC2-200406111814/eclipse-SDK-3.0RC2-linux-motif.zip
<?php } else if($platform == "gtk") { ?>
http://download2.eclipse.org/downloads/drops/S-3.0RC2-200406111814/eclipse-SDK-3.0RC2-linux-gtk.zip
<?php } else if($platform == "carbon") { ?>
http://download2.eclipse.org/downloads/drops/S-3.0RC2-200406111814/swt-3.0RC2-macosx-carbon.zip
<?php		}
	} else {
	
	echo($latestVersion . "\n");
	
	if($platform == "win32") { ?>
http://mirror.tiscali.dk/eclipse/downloads/drops/S-3.0M9-200405211200/swt-3.0M9-win32.zip
http://download2.eclipse.org/downloads/drops/S-3.0M9-200405211200/swt-3.0M9-win32.zip
<?php } else if($platform == "motif") { ?>
http://mirror.tiscali.dk/eclipse/downloads/drops/S-3.0M9-200405211200/swt-3.0M9-linux-motif.zip
http://download2.eclipse.org/downloads/drops/S-3.0M9-200405211200/swt-3.0M9-linux-motif.zip
<?php } else if($platform == "gtk") { ?>
http://mirror.tiscali.dk/eclipse/downloads/drops/S-3.0M9-200405211200/swt-3.0M9-linux-gtk.zip
http://download2.eclipse.org/downloads/drops/S-3.0M9-200405211200/swt-3.0M9-linux-motif.zip
<?php } else if($platform == "carbon") { ?>
http://mirror.tiscali.dk/eclipse/downloads/drops/S-3.0M9-200405211200/swt-3.0M9-macosx-carbon.zip
http://download2.eclipse.org/downloads/drops/S-3.0M9-200405211200/swt-3.0M9-macosx-carbon.zip
	<?php } ?>
<?php } ?><?php
	$latestVersion = 3052;

	if(! isset($platform) || $platform == '')
		$platform 	= @$_GET['platform'];
		
	if(! isset($platform) || $platform == '')
		exit();
		
	if(! isset($version) || $version == '')
		$version 	= @$_GET['version'];	
		
	if(! isset($version))
		$version 	= '';	
	
	if(substr($version,0, 7) == "2.1.0.1") {
	
	echo("3058\n");
	
	if($platform == "win32") { ?>
http://download2.eclipse.org/downloads/drops/S-3.0RC2-200406111814/swt-3.0RC2-win32.zip
<?php } else if($platform == "motif") { ?>
http://download2.eclipse.org/downloads/drops/S-3.0RC2-200406111814/swt-3.0RC2-linux-motif.zip
<?php } else if($platform == "gtk") { ?>
http://download2.eclipse.org/downloads/drops/S-3.0RC2-200406111814/swt-3.0RC2-linux-gtk.zip
<?php } else if($platform == "carbon") { ?>
http://download2.eclipse.org/downloads/drops/S-3.0RC2-200406111814/swt-3.0RC2-macosx-carbon.zip
<?php		}
	} else {
	
	echo($latestVersion . "\n");
	
	if($platform == "win32") { ?>
http://mirror.tiscali.dk/eclipse/downloads/drops/S-3.0M9-200405211200/swt-3.0M9-win32.zip
http://download2.eclipse.org/downloads/drops/S-3.0M9-200405211200/swt-3.0M9-win32.zip
<?php } else if($platform == "motif") { ?>
http://mirror.tiscali.dk/eclipse/downloads/drops/S-3.0M9-200405211200/swt-3.0M9-linux-motif.zip
http://download2.eclipse.org/downloads/drops/S-3.0M9-200405211200/swt-3.0M9-linux-motif.zip
<?php } else if($platform == "gtk") { ?>
http://mirror.tiscali.dk/eclipse/downloads/drops/S-3.0M9-200405211200/swt-3.0M9-linux-gtk.zip
http://download2.eclipse.org/downloads/drops/S-3.0M9-200405211200/swt-3.0M9-linux-motif.zip
<?php } else if($platform == "carbon") { ?>
http://mirror.tiscali.dk/eclipse/downloads/drops/S-3.0M9-200405211200/swt-3.0M9-macosx-carbon.zip
http://download2.eclipse.org/downloads/drops/S-3.0M9-200405211200/swt-3.0M9-macosx-carbon.zip
	<?php } ?>
<?php } ?>