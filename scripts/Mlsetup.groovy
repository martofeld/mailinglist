
import grails.util.Metadata
import groovy.text.SimpleTemplateEngine

import org.apache.commons.io.FileUtils



includeTargets << grailsScript("_GrailsArgParsing")

USAGE = """
    Mlsetup org.example.com 5
    org.example.com = The package name to install malinglist classes to.
	5 = number of jobs to create for mailinglist 
"""

target (Mlsetup: 'Sets up a new maling list project' ) {

	/*if(new File(basedir, "grails-app/conf/MailingListConfig.groovy").exists()) {
		println "MailingListConfig.groovy exists, try removing this config file to reinstall."
		println "Nothing changed."
		exit(1)
	}*/

	
	def (pack,amount) = parseArgs()
	def packdir = pack.replace('.', '/')
		
	// TODO -- Convention needs is so that MailingList is replaced to defined convention to match user needs
	// convention is just taken as input so that input format does not change in the future
	
	def mlbind = [ 'pack':pack, 'classname':'MailingList', 'baseclassname':'MailingListBase' ]
	def mlFrom = [ 'pack':pack, 'classname':'MailingListFrom', 'baseclassname':'FromBase' ]
	def mlCat = [ 'pack':pack, 'classname':'MailingListCategories', 'baseclassname':'CategoryBase' ]
	def mlAtt = [ 'pack':pack, 'classname':'MailingListAttachments', 'baseclassname':'AttachmentsBase' ]
	def mlSched = [ 'pack':pack, 'classname':'MailingListSchedule', 'baseclassname':'ScheduleBase' ]
	def mlTemp = [ 'pack':pack, 'classname':'MailingListTemplates', 'baseclassname':'TemplatesBase' ]
	def mlSend = [ 'pack':pack, 'classname':'MailingListSenders', 'baseclassname':'SendersBase' ]
	
	def controllerConf = [ 'pack':pack ]
	def serviceConf= [ 'pack':pack ,'amount':amount ]
	
	
	def engine = new SimpleTemplateEngine()
	
	// Domains
	def mailingTemplate = engine.createTemplate(new FileReader("${mailinglistPluginDir}/src/templates/domain/default.groovy")).make(mlbind)
	def mtFrom = engine.createTemplate(new FileReader("${mailinglistPluginDir}/src/templates/domain/default.groovy")).make(mlFrom)
	def mtCat = engine.createTemplate(new FileReader("${mailinglistPluginDir}/src/templates/domain/default.groovy")).make(mlCat)
	def mtAtt = engine.createTemplate(new FileReader("${mailinglistPluginDir}/src/templates/domain/default.groovy")).make(mlAtt)
	def mtSched = engine.createTemplate(new FileReader("${mailinglistPluginDir}/src/templates/domain/default.groovy")).make(mlSched)
	def mtSend = engine.createTemplate(new FileReader("${mailinglistPluginDir}/src/templates/domain/default.groovy")).make(mlSend)
	def mtTemp = engine.createTemplate(new FileReader("${mailinglistPluginDir}/src/templates/domain/default.groovy")).make(mlTemp)
	
	
	//Controllers
	def EmailController = engine.createTemplate(new FileReader("${mailinglistPluginDir}/src/templates/controllers/EmailController.groovy")).make(controllerConf)
	def MailingListAttachmentsController = engine.createTemplate(new FileReader("${mailinglistPluginDir}/src/templates/controllers/MailingListAttachmentsController.groovy")).make(controllerConf)
	def MailingListCategoriesController = engine.createTemplate(new FileReader("${mailinglistPluginDir}/src/templates/controllers/MailingListCategoriesController.groovy")).make(controllerConf)
	def MailingListScheduleController = engine.createTemplate(new FileReader("${mailinglistPluginDir}/src/templates/controllers/MailingListScheduleController.groovy")).make(controllerConf)
	def MailingListController = engine.createTemplate(new FileReader("${mailinglistPluginDir}/src/templates/controllers/MailingListController.groovy")).make(controllerConf)
	def MailingListSendersController = engine.createTemplate(new FileReader("${mailinglistPluginDir}/src/templates/controllers/MailingListSendersController.groovy")).make(controllerConf)
	def MailingListTemplatesController = engine.createTemplate(new FileReader("${mailinglistPluginDir}/src/templates/controllers/MailingListTemplatesController.groovy")).make(controllerConf)
	def MailingListUploaderController = engine.createTemplate(new FileReader("${mailinglistPluginDir}/src/templates/controllers/MailingListUploaderController.groovy")).make(controllerConf)
	
	//Services
	def EmailService = engine.createTemplate(new FileReader("${mailinglistPluginDir}/src/templates/services/EmailService.groovy")).make(controllerConf)
	def qss = engine.createTemplate(new FileReader("${mailinglistPluginDir}/src/templates/services/QuartzStatusService.groovy")).make(controllerConf)
	
	
	
	println "Lets go los los"
	
	StringBuilder cad = new StringBuilder()
	// Create jobs
	if (amount) {
		def appName=Metadata.getCurrent().'app.name' 
		def jobPath="${basedir}/grails-app/jobs/${appName}"
		mkdir(dir:"${jobPath}")
		
		for (i=0; i < amount; i++) {
			def jobName="ScheduleEmail${i}Job"
			println "Creating job ${i} job Name: grails-app/jobs/${appName}/${jobName}.groovy"  
			def cjob = [ 'pack':appName, 'classname':jobName ]
			def mtJob = engine.createTemplate(new FileReader("${mailinglistPluginDir}/src/templates/jobs/ScheduleEmailJob.groovy")).make(cjob)
			new File(basedir, "grails-app/jobs/${appName}/${jobName}.groovy").write(mtJob.toString())
			println "Generating EmailCheckerService Schedule Pool "+jobName
			cad.append('\t\t\t\t\tif (i=='+i+') {\n')
			cad.append('\t\t\t\t\t\t'+jobName+'.schedule(scheduledDate, paramsMap)\n')
			cad.append('\t\t\t\t\t\tsb.append("'+jobName+'")\n')
			cad.append('\t\t\t\t\t\tbreak\n')
			cad.append('\t\t\t\t\t}\n')
			
		}
	}
	def checkerConf= [ 'pack':pack ,'amount':amount, 'jobMapping':cad ]
	def qecs = engine.createTemplate(new FileReader("${mailinglistPluginDir}/src/templates/services/QuartzEmailCheckerService.groovy")).make(checkerConf)
	
	
	String sourceDir = "${mailinglistPluginDir}/src/templates/views"
	String destinationDir = "${basedir}/grails-app/"
	dir = new File(sourceDir)
	dir2 = new File(destinationDir	)
	dir.eachFileRecurse{ f -> if (f.isDirectory()) println 'Creating views folder: '+f.name}
	
	
	FileUtils.copyDirectoryToDirectory(dir,dir2)
	StringBuilder imf=new StringBuilder()
	imf.append('<%@ page import="'+pack+'.MailingListCategories" %>\n')
	imf.append('<%@ page import="'+pack+'.MailingListAttachments" %>\n')
	imf.append('<%@ page import="'+pack+'.MailingListSenders" %>\n')
	imf.append('<%@ page import="'+pack+'.MailingListTemplates" %>\n')
	imf.append('<%@ page import="'+pack+'.MailingList" %>\n')
	new File(basedir, "grails-app/views/_import.gsp").write(imf.toString())
	
	
	mkdir(dir:"${basedir}/grails-app/domain/${packdir}")
	
	mkdir(dir:"${basedir}/grails-app/controllers/${packdir}")
	println "Creating domain classes within your project: grails-app/domain/${packdir}"
	new File(basedir, "grails-app/domain/${packdir}/MailingList.groovy").write(mailingTemplate.toString())
	new File(basedir, "grails-app/domain/${packdir}/MailingListCategories.groovy").write(mtCat.toString())
	new File(basedir, "grails-app/domain/${packdir}/MailingListAttachments.groovy").write(mtAtt.toString())
	new File(basedir, "grails-app/domain/${packdir}/MailingListFrom.groovy").write(mtFrom.toString())
	new File(basedir, "grails-app/domain/${packdir}/MailingListSchedule.groovy").write(mtSched.toString())
	new File(basedir, "grails-app/domain/${packdir}/MailingListTemplates.groovy").write(mtTemp.toString())
	new File(basedir, "grails-app/domain/${packdir}/MailingListSenders.groovy").write(mtSend.toString())
	
	println "Creating controllers within your project: grails-app/controllers/${packdir}"
	new File(basedir, "grails-app/controllers/${packdir}/EmailController.groovy").write(EmailController.toString())
	new File(basedir, "grails-app/controllers/${packdir}/MailingListAttachmentsController.groovy").write(MailingListAttachmentsController.toString())
	new File(basedir, "grails-app/controllers/${packdir}/MailingListCategoriesController.groovy").write(MailingListCategoriesController.toString())
	new File(basedir, "grails-app/controllers/${packdir}/MailingListController.groovy").write(MailingListController.toString())
	new File(basedir, "grails-app/controllers/${packdir}/MailingListScheduleController.groovy").write(MailingListScheduleController.toString())
	new File(basedir, "grails-app/controllers/${packdir}/MailingListSendersController.groovy").write(MailingListSendersController.toString())
	new File(basedir, "grails-app/controllers/${packdir}/MailingListTemplatesController.groovy").write(MailingListTemplatesController.toString())
	new File(basedir, "grails-app/controllers/${packdir}/MailingListUploaderController.groovy").write(MailingListUploaderController.toString())
	
	println "Creating Services within your project: grails-app/services/${packdir}"
	mkdir(dir:"${basedir}/grails-app/services/${packdir}")
	new File(basedir, "grails-app/services/${packdir}/EmailService.groovy").write(EmailService.toString())
	new File(basedir, "grails-app/services/${packdir}/QuartzEmailCheckerService.groovy").write(qecs.toString())
	new File(basedir, "grails-app/services/${packdir}/QuartzStatusService.groovy").write(qss.toString())

	println """Completed to finalise add compile ":quartz-monitor:0.3-RC3"  to your BuildConfig.groovy and refresh dependencies"""
}

def parseArgs() {
	args = args ? args.split('\n') : []
	if (2 == args.size()) {
		println "Using package ${args[0]} to create custom classes"
		println "Using numeric ${args[1]} to create amount of jobs"
		return [args[0], args[1] as int]
	}
	usage()
}

private void usage() {
	println "Usage:\n${USAGE}"
	exit(1)
}

setDefaultTarget 'Mlsetup'