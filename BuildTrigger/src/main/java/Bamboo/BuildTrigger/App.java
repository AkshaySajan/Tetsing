package Bamboo.BuildTrigger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.model.CauseOfInterruption;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
public class App extends Builder 
{
	
    private String projectKey;
    private String planKey;
    private String serverAddress;
    private String username;
    private String password;
    private String msg;
	
	 @DataBoundConstructor
	 public App(String projectKey, String planKey, String serverAddress, String username, String password) {
		 	this.projectKey = projectKey;
	        this.planKey = planKey;
	        this.serverAddress = serverAddress;
	        this.username = username;
	        this.password = password;
	 }
	  public String getProjectKey() {
		return projectKey;
	}

    public void setProjectKey(String projectKey) {
		this.projectKey = projectKey;
	}





	public String getMsg() {
		return msg;
	}
	public void setMsg(String msg) {
		this.msg = msg;
	}
	public String getPlanKey() {
		return planKey;
	}





	public void setPlanKey(String planKey) {
		this.planKey = planKey;
	}





	public String getServerAddress() {
		return serverAddress;
	}





	public void setServerAddress(String serverAddress) {
		this.serverAddress = serverAddress;
	}





	public String getUsername() {
		return username;
	}





	public void setUsername(String username) {
		this.username = username;
	}





	public String getPassword() {
		return password;
	}





	public void setPassword(String password) {
		this.password = password;
	}

	private static String bambooPostResult;
	private static String bamboogetResult;
	
	@Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

    	@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			// TODO Auto-generated method stub
			return true ;
		}


        @Override
        public String getDisplayName() {
            return "Trigger Bamboo Build";
        }
    }
	
	 public boolean perform(Build<?, ?> build, Launcher launcher, BuildListener listener)
 			throws InterruptedException, IOException {
		
        int buildNumber = -1;
		String serverUrl = this.getServerAddress();
		String planKey = this.getPlanKey();
		String projectKey = this.getProjectKey();
		String username = this.getUsername();
		String password = this.getPassword();
		String triggerApi = "/rest/api/latest/queue/";
		String buildStatusApi = "/rest/api/latest/result/";
        String triggerUrl = serverUrl + triggerApi;
        String buildStatusUrl = serverUrl + buildStatusApi;
        
        Result bresult = Result.NOT_BUILT;
        
        int status = bambooPost(triggerUrl, username, password, projectKey, planKey);
        listener.getLogger().println("Build Trigger Result: " + bambooPostResult);
        listener.getLogger().println("================================================================");
        if(status == 200) {
        	  buildNumber = getbuildNumber(bambooPostResult);
        	  boolean buildStatus = checkBuildStatus(buildNumber, planKey,projectKey, buildStatusUrl,username,password);
        	  listener.getLogger().println("Build State Result: " + bamboogetResult);
        	  
        	  
        	  if(buildStatus == false) {
              	build.setResult(bresult.FAILURE);
				}else {
					build.setResult(bresult.SUCCESS);
              }
        }
     
      
        return true;
    }
	
	
	public static boolean checkBuildStatus (int buildNumber, String planKey, String projectKey, String serverUrl,String username, String password) {
		   Map <String, String> buildStatus = (Map<String, String>) getBuildStatus(buildNumber, planKey, projectKey,serverUrl,username,password);
		   
		  		 
		  		while (!(buildStatus.get("lifeCycleState").toString()).equals("Finished")) {
					  buildStatus = (Map<String, String>) getBuildStatus(buildNumber, planKey, projectKey,serverUrl,username,password);
					   System.out.println(buildStatus);

				   }
					   
					   if(buildStatus.get("state").toString().equals("Successful")) {
						   return true;
					   }else {
						   return false;
					   }
					
		  	}
		  	
		  	
		   
			  
	   
    
   public static Map<?,?> getBuildStatus(int buildNumber, String planKey, String projectKey, String serverUrl,String username, String password) {
	   String buildStatus;
	   Map<String, String> result = new HashMap<>();
	   	String getUrl = serverUrl + projectKey + "-" + planKey + "-" + buildNumber + ".json";
	   	HttpClient client = new HttpClient();
		  

	   	Credentials credentials = new UsernamePasswordCredentials(username, password);
	   	client.getState().setCredentials(AuthScope.ANY, credentials);
	   	GetMethod get = new GetMethod(getUrl);
	   	
	   	int status;
	   	ObjectMapper objectMapper = new ObjectMapper();
	   	
	   	try {
	   		status = client.executeMethod(get);
	   		//("GET Status Code: " + status + " GET url: " + getUrl);
		   	InputStream in = get.getResponseBodyAsStream();
		   	bamboogetResult = IOUtils.toString(in, StandardCharsets.UTF_8);
		   	JsonNode node = objectMapper.readValue(bamboogetResult, JsonNode.class);
		   	result.put("state", node.get("buildState").asText());
		   	result.put("lifeCycleState", node.get("lifeCycleState").asText());
	   		System.out.println(bamboogetResult);
	   		
	   	} catch (Exception e) {
	   		System.out.println("Error: " + e);
	   	}
	   	
	   return result;
	   
   }

    
    public static int bambooPost(String url, String username, String password, String projectKey, String planKey) {
    	
    	HttpClient client = new HttpClient();
    	Credentials credentials = new UsernamePasswordCredentials(username, password);
    	client.getState().setCredentials(AuthScope.ANY, credentials);
    	
		
    	int status = 504;
    	
    	String postUrl = url + projectKey + "-" + planKey + ".json?stage&executeAllStages&os_authType=basic";
    	PostMethod post = new PostMethod(postUrl);
    	
    	 try {
             status = client.executeMethod(post);

             System.out.println("POST Status Code: " + status + " Post url: " + postUrl);
             InputStream in = post.getResponseBodyAsStream();
             bambooPostResult = IOUtils.toString(in, StandardCharsets.UTF_8);
             
         } catch(IOException e) {
             System.out.println("Could not execute POST due to IOException.");
         } finally {
             post.releaseConnection();
         }
    	
    	return status;
    }
    
    public static int getbuildNumber(String result) {
    	System.out.println(result);
    	int buildNumber = -1;
    	ObjectMapper objectMapper = new ObjectMapper();
    	
    	try {
    		
    		JsonNode node = objectMapper.readValue(result, JsonNode.class);
    		if(node.has("buildNumber")) {
    			buildNumber = node.get("buildNumber").asInt();
    		} else {
    			System.out.println("Couldnt get the build number");
    			return buildNumber;
    		}
    	} catch (Exception e) {
    		System.out.println(e);
    	}
    		
    	return buildNumber;
    }
}
