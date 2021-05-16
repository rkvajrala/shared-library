def call(Map config= [:])
{ 
	
	sh "echo Start setting env and deployment config variables"

	sh "echo cURL :  ${config}"

	envVariables = [
	 					"cloneURL" : ${config.cURL},
	 					"cloneBranch" : ${config.cBranch},
       					"email" : ${config.email}
	 				]
	 
	/*deployProperties = [

					"businessGroupName" : ${config.bgName},
					"targetEnvironmentName" : ${config.teName},
					"numberOfWorkers" : ${config.nofWorkers},
					"workerSize" : ${config.wSize},
					"deployRegion" : ${config.dRegion},
					"deployNamePrefix" : ${config.dnPrefix},
					"muleKeyId" : ${config.mkId}
				]

  */
	sh "echo End of setting env and deployment config variables"
}
