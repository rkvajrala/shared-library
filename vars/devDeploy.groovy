import groovy.json.JsonSlurper
import groovy.json.*
import groovy.json.JsonSlurperClassic


def call(Map config= [:])
{ 
	
	sh "echo Start setting env and deployment config variables"

	
	envVariables = [
	 					"cloneURL" : config.cURL,
	 					"cloneBranch" : config.cBranch,
       					"email" : config.email
	 				]
	 sh "echo envVariables : ${envVariables}"
	deployProperties = [

					"businessGroupName" : config.bgName,
					"targetEnvironmentName" : config.teName,
					"numberOfWorkers" : config.nofWorkers,
					"workerSize" : config.wSize,
					"deployRegion" : config.dRegion,
					"deployNamePrefix" : config.dnPrefix,
					"muleKeyId" : config.mkId
				]

  	sh "echo deployVariables : ${deployProperties}"
	sh "echo End of setting env and deployment config variables"

	sh "echo cloneURL : ${envVariables.cloneURL}"

	rulesVersion = "1.0.0"


	node
	{

		stage('CloneRepo')
		{
			git branch: envVariables.cloneBranch,
				url: envVariables.cloneURL

			groupId = readMavenPom().getGroupId()
			artifactId = readMavenPom().getArtifactId()
			version = readMavenPom().getVersion()

			runtimeVersion = readMavenPom().getProperties().getProperty('app.runtime')
			
			dPrefix = readMavenPom().getProperties().getProperty('deployment.prefix')
			dSuffix = readMavenPom().getProperties().getProperty('deployment.suffix')
			dName = artifactId

						
			sh "echo groupId : ${groupId}"	

			println("== Sucessfully clone the source code in to jenkins workspace from codeCloud ==") 		
						

		}
		stage('Get Application Properties') 
		{
			def configPath = 'src/main/resources/' + deployProperties.targetEnvironmentName +'.yaml'
			def yamlProps = readYaml file: "${WORKSPACE}/${configPath}"
			def jsonProps = JsonOutput.toJson(yamlProps)
			def parsedJson = new JsonSlurperClassic().parseText(jsonProps)
			 appProperties = commonUtils.getProperties(parsedJson)
						
			//sh'''
			//				rm -r $WORKSPACE/external-properties
			//'''''

			sh "echo workspace : ${WORKSPACE}/${configPath}"
			sh "echo appProperties : ${appProperties}"
			
			println("== Sucessfully Read the properties from location external-properties/config-" + deployProperties.targetEnvironmentName + ".yaml ==")
				
		}
		stage("Maven Package and MUnit testing") 
		{
         
          	def gitTag = sh(returnStdout: true, script: "git tag --contains | head -1").trim()
         
            def gitBranch = envVariables.cloneBranch
         
            def builtOn = new Date().format("yyyy-MM-dd.HH:mm:ss", TimeZone.getTimeZone('UTC'))
         
            def builtBy = sh(returnStdout: true, script: "git log -n 1 --pretty=format:'%an'").trim()
         
            def buildInfo = JsonOutput.toJson([buildInformation:[buildNumber: "${BUILD_NUMBER}", gitTag: gitTag ,gitBranch: gitBranch, builtOn: builtOn, builtBy: builtBy]])
         
            println("== buildInfo == " + buildInfo)
    

           	writeJSON file: 'src/main/resources/META-INF/build-metadata/buildInfo.json', json: buildInfo
	
		 	withMaven(jdk: 'jdk8', maven: 'maven', mavenSettingsConfig: 'MAVEN_SETTINGS') {
		 		sh '''echo
		 		pwd
		 		ls -l
		 		which mvn
		 		mvn clean package -Dbuild.number=${BUILD_NUMBER} -Dmule.env=dev
							
		 		'''
		 		println("== Maven Package Build sucessfully completed==")
		 	}

		}
		stage('Send Artifact to Nexus Repository'){
					 withMaven(jdk: 'jdk8', maven: 'maven', mavenSettingsConfig: 'MAVEN_SETTINGS') 
					 {	
						
							sh "mvn deploy:deploy-file -DgroupId=${groupId} -DartifactId=${artifactId} -Dversion=B${BUILD_NUMBER}-${version} -DgeneratePom=true -Dpackaging=jar -Dfile=${WORKSPACE}/target/${artifactId}-B${BUILD_NUMBER}-${version}-mule-application.jar"
							jarName = "${artifactId}-B${BUILD_NUMBER}-${version}-mule-application.jar"
						
						println("== Artifact successfully deployed to Nexus repository. Deployed file name is :" + jarName + " ==")
					}
				}
				/*
		stage("Obtain Anypoint Token") {
						creds = commonUtils.getCredsNonProd() // anypointUsername and anypointPassword
						println("Credential Used For Anypoint Platform: " + creds.anypointUsernameNP)
						authorization = commonUtils.getAnypointToken(creds.anypointUsernameNP, creds.anypointPasswordNP)
						println("Credential Used For Anypoint Platform: " + authorization)
		}

		stage("Read Environment and Business Groups from the stored file"){
						println("== Business Group Name is : " + deployProperties.businessGroupName + " ==")
						def getSourceIfo = commonUtils.getBGIDandEID(deployProperties.businessGroupName, deployProperties.targetEnvironmentName)
						sourceEnvironmentId = getSourceIfo.environmentId
						businessGroupId = getSourceIfo.businessGroupId
						targetProperties = [ "clientId" : getSourceIfo.clientId, "clientSecret" : getSourceIfo.clientSecret]
						println("== Business Group Id is : " + businessGroupId + " ==")
						println("== EnvId is : " + sourceEnvironmentId + " ==")
				}
				stage("Get Deployed apps list from Target Environment") {
						
						def getsourceDeployedApps = commonUtils.getListDeployedApps(authorization, businessGroupId, sourceEnvironmentId)
						println("== Sucessfully retrived the list of applications deployed in requested environment : " + deployProperties.targetEnvironmentName + " ==")
						sourceDeployemntName = deployProperties.deployNamePrefix + "-" + deployProperties.targetEnvironmentName.toLowerCase() + "-" + dName
						println("=== sourceDeployemntName: " +sourceDeployemntName)
						alreadyDeployed = getsourceDeployedApps.data.id.contains(sourceDeployemntName)
						println("== Application already Deployed ? : " + alreadyDeployed + " ==")
				}
				stage("Deploy Application to Target Environment") {
						println("== Application already Deployed ? : " + alreadyDeployed + " ==")
						if(alreadyDeployed == true) {
							deployMode = "redeploy" 
						}
						else { 
						   
						   deployMode= "deploy" 
					   }
					   
					   println("=~~~Deploy Mode is : " + deployMode + " ==")
					   
					   def deployProp = ""
						def applicationAPIID = ""
						if (applicationAPIID != "") {
							println("applicationAPIID found :  " + applicationAPIID)
							deployProp = ['env': deployProperties.targetEnvironmentName, 'anypoint.platform.client_id' : targetProperties.clientId, 'anypoint.platform.client_secret' : targetProperties.clientSecret, 'ignore.local.config.file' : true]
							
						} else {

							println("applicationAPIID Not found :  " + applicationAPIID)
							
							deployProp = ['env': deployProperties.targetEnvironmentName, 'anypoint.platform.client_id' : targetProperties.clientId, 'anypoint.platform.client_secret' : targetProperties.clientSecret, 'ignore.local.config.file' : true]
						}
											   
					   def appName = sourceDeployemntName
					   
					   //def deployProp = ['mule.env': deployProperties.targetEnvironmentName, 'mule.key': Mule_KEY, 'anypoint.platform.client_id' : targetProperties.clientId, 'anypoint.platform.client_secret' : targetProperties.clientSecret, 'ignore.local.config.file' : true]
			 
					   def properties = appProperties << deployProp

					   def appInfoJson = JsonOutput.toJson([domain : "${appName}", muleVersion:[version:runtimeVersion],region:deployProperties.deployRegion,monitoringEnabled:true,monitoringAutoRestart:true,properties: properties, workers:[amount:deployProperties.numberOfWorkers,type:[name:deployProperties.workerSize]],loggingNgEnabled:true])
					   def fileName = jarName
					   println("== Before Deploying the Application ==")
					   
					   def deployApplicationResponse = commonUtils.deployApplication(authorization, businessGroupId, sourceEnvironmentId, appInfoJson, fileName, appName, deployMode, deployProperties.targetEnvironmentName)
								   
						println("== Deployment is done1 ==")
						println("== Dev Pipeline execution is completed1 ==")
				}*/

	}
	

}
