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

		stage('Stage1')
		{
			sh "echo cloneURL : ${envVariables.cloneURL}"

		}

	}
	

}
