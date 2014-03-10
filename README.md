mailinglist 0.1
=======================


mailinglist is a Grails plugin which makes use of quartz to dynamically schedule either group or specific email address contact.
You create html email templates with images etc, then define time and date for this to be sent. The job is then added to quartz and set to email at given time.
The queue can easily be controlled via bootstrap so that nothing is ever lost.

Do you want to email a person at 11:41 pm or maybe a group of people at 2.15am? then look no further.

You can schedule an email to be scheduled and to run on a set date and time. 

Supports HTML emails with inline images as well as attachments has been tested on outlook and result appears to load fine.



WARNING ! - this will copy over controllers/views/domainClasses/services and create Quartz jobs within your project, if this is your first time using this plugin, it would be advised to create a new project and test the plugin, before attempting to install it on main project.

This plugin is still under development and has not been released


For a walk through guide on how to install this plugin goto : https://github.com/vahidhedayati/ml-test

## Installation:
Add plugin Dependency in BuildConfig.groovy :

	compile ":mailinglist:0.1"


#### BuildConfig.groovylayout/main.gsp update:

##### jquery, jquery-ui libraries:
your layouts main.gsp: (add jquery-ui and jquery - or add them into ApplicationResources.groovy and ensure you refer to it in your main.gsp or relevant file

	<g:javascript library="jquery-ui"/>
	<g:javascript library="jquery"/>
	…
	<g:layoutHead/>



Now with that all in place open grails console or from the command line run

	grails mlsetup org.example.com 5

	Where org.example.com is your package and 5 is the amount of dynamic schedule jobs to generate,

Assuming package was labelled as above org.example.com and schedule jobs as 5, install script will create

	Controllers under org.example.com
	Domains 	under org.example.com
	Views 		under views/mailingList[a-z]

	Jobs under 	org.example.com/ScheduleEmail0Job.groovy
			   	org.example.com/ScheduleEmail1Job.groovy
			   	org.example.com/ScheduleEmail2Job.groovy
			   	org.example.com/ScheduleEmail3Job.groovy
			   	org.example.com/ScheduleEmail4Job.groovy

	Services 	under org.example.com
				It will update QuartzEmailCheckerService to only schedule physical jobs ScheduleEmail[0-4]Job


The domains generated in your application extend base domains within plugin, besides this the rest of the controllers etc are pushed to your own application for you to do what you like with it.



#### Config.groovy changes

Required Config.groovy configurations:


	// Optional values to override DB table names for this plugin:
	//mailinglist.table.schedule='MyScheduler'
	//mailinglist.table.attachments='something'
	//mailinglist.table.categories='something'
	//mailinglist.table.from='something'
	//mailinglist.table.mailinglist='something'
	//mailinglist.table.schedule='something'
	//mailinglist.table.senders='something'
	//mailinglist.table.templates='something'
	
	// Your date format that matches input of jquery datepicker config 
	//mailinglist.dtFormat='dd/MM/yyyy HH.mm'


	ckeditor {
		//config = "/js/myckconfig.js"
			skipAllowedItemsCheck = false
		defaultFileBrowser = "ofm"
		upload {
			//basedir = "/uploads/"

			baseurl="${grails.baseURL}"+'/uploads/'
			basedir = "${externalUploadPath}"
				overwrite = false
				link {
					browser = true
					upload = false
					allowed = []
					denied = ['html', 'htm', 'php', 'php2', 'php3', 'php4', 'php5',
							  'phtml', 'pwml', 'inc', 'asp', 'aspx', 'ascx', 'jsp',
							  'cfm', 'cfc', 'pl', 'bat', 'exe', 'com', 'dll', 'vbs', 'js', 'reg',
							  'cgi', 'htaccess', 'asis', 'sh', 'shtml', 'shtm', 'phtm']
				}
				image {
					browser = true
					upload = true
					allowed = ['jpg', 'gif', 'jpeg', 'png']
					denied = []
				}
				flash {
					browser = false
					upload = false
					allowed = ['swf']
					denied = []
				}
		}
	}
	jqueryDateTimePicker {
		format {
			java {
				datetime = "dd/MM/yyyy HH.mm"
				date = "dd/MM/yyyy"
			}
			picker {
				date = "'dd/mm/yy'"
				time = "'H.mm'"
			}
		}
	}

	grails.mime.types = [ html: ['text/html','application/xhtml+xml'],
		xml: ['text/xml', 'application/xml'],
		text: 'text-plain',
		js: 'text/javascript',
		rss: 'application/rss+xml',
		atom: 'application/atom+xml',
		css: 'text/css',
		csv: 'text/csv',
		pdf: 'application/pdf',
		rtf: 'application/rtf',
		excel: 'application/vnd.ms-excel',
		ods: 'application/vnd.oasis.opendocument.spreadsheet',
		all: '*/*',
		json: ['application/json','text/json'],
		form: 'application/x-www-form-urlencoded',
		multipartForm: 'multipart/form-data'
	  ]



You will notice 	grails.baseURL externalUploadPath within ckeditor, this was done to externalise image uploads so upon a redployment the images were still available, the approach I took to this was to run values from setenv.sh within tomcat and pass this values in as variables into config.groovy as per below:


// configuration for plugin testing - will not be included in the plugin zip


// In my tomcat setenv.sh

	//UPLOADLOC="$CATALINA_HOME/uploads"
	//JAVA_OPTS="$JAVA_OPTS -DUPLOADLOC=$UPLOADLOC"
	//HOSTNAME=$(hostname)
	//JAVA_OPTS="$JAVA_OPTS -DSERVERURL=$HOSTNAME"


Produces running tomcat with the following values:

	// -DUPLOADLOC=/opt/tomcat7/tc1/uploads
	// -DSERVERURL=my.server.com


In my Config.groovy at the top I have this

	if (System.getProperty('UPLOADLOC')) {
		externalUploadPath=System.getProperty('UPLOADLOC')+File.separator
	}
	if (System.getProperty('SERVERURL')) {
		grails.baseURL='http://'+System.getProperty('SERVERURL')
	}else{
		grails.baseURL='http://localhost'
	}

Now those values are valid within the ckeditor configuration



#### Boostrap changes

An example BootStrap call to requeue outstanding or interuppted schedules is to add something like this :


    class BootStrap {
        ..
        def mailingListEmailService

        ..
        def getEmails = MailingListSchedule.findAllByScheduleCompleteAndScheduleCancelled(false,false)
        getEmails.each { params ->
            if (params.dateTime && params.emailMessage) {
                println "RESCHEDULING MAIL QUEUE ${params?.id} --       ${params?.mailFrom}---${params?.recipientToGroup}--${params?.recipientToList}"
                mailingListEmailService.rescheduleit(params)
            }
        }
        ..
    }
 
 
    
#### Pop up Modal boxes within contact clients
 
 I have ammended the plugin to include bootstrap-min.js & ml-boostrap-combined.min.css, 
  the plugin will write the js file only if does not already exist within your local web-apps/js folder.
  The new css is added and referred to within contactclients.gsp where the same js file is also called.
  This then allows modal popups to work within the standard grails framework. 
  I ammended the css file from the standard to remove a large chunk from the top so that the default 
  grails site still worked as per normal. The standed boostrap css shifts the site to the left.
 
New Email is a fully functional updater that just updates div, issues with it poping open again, 
spent ages on this and going to leave it alone for now.
The rest of the green buttons on the contact page all add elements and do a redirect back to contact page. 
The only thing you really loose on this page is the subject since everything else is select or tick.



#### Future upgrades etc

This project at the moment installs nearly everything to your local project which means you have great power over changes to how things work. This does mean future updates i.e. by running the install script will overwrite everything including changes to anything local to this project made on your local copy.
Advice for now if or until method changes is to always backup your existing project before either running the mlsetup to increase pool or running to upgrade upon future releases. 
If you are not going to make any changes to the plugin code then nothing to worry about update as much as required. 


## Finally

A big thank you as always to Burt Beckwith for forking and fixing up a lot of my wild wild west code :). 
Unfortunately due to issues with merging back the original fork and after a few attempts to fix the issue I decided to rm -rf git repo and upload correct bit which included his changes. 
Those changes are here: https://github.com/burtbeckwith/mailinglist/

   
 

    
