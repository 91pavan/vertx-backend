package com.cisco.cmad.vertx_backend;

import java.io.IOException;
import java.util.List;

import org.mongodb.morphia.Datastore;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.vertx.ext.mongo.MongoClient;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;

/**
 * Hello world!
 *
 */
public class App extends AbstractVerticle
{
	
	private MongoClient client;
	private String userName = null;
	private String siteId = null;
	private String companyId = null;
	private String deptId = null;
    public static String cId = null;
    public static String sId = null;
    public static String dId = null;
    
    SessionStore store1 = null;

	
    JsonObject userObj = null;
    
	public void start(Future<Void> startFuture) {
		
    	System.out.println("My verticle started!");
    	JsonObject config = new JsonObject()
        .put("connection_string", "mongodb://localhost:27017")
        .put("db_name", "cmad");
    	client = MongoClient.createShared(vertx, config);
    	HttpServer();
    	store1 = LocalSessionStore.create(vertx);
    	startFuture.complete();
    }
    
    @Override
    public void stop(Future stopFuture) throws Exception{
    	System.out.println("My verticle stopped!");
    }
	
    public void HttpServer() {
    	HttpServer server = vertx.createHttpServer();
    	Router router = Router.router(vertx);
    	router.route().handler(BodyHandler.create());
    	
    	// cors
    	router.route().handler(CorsHandler.create("*")
    		      .allowedMethod(HttpMethod.GET)
    		      .allowedMethod(HttpMethod.POST)
    		      .allowedMethod(HttpMethod.PUT)
    		      .allowedMethod(HttpMethod.DELETE)
    		      .allowedMethod(HttpMethod.OPTIONS)
    		      .allowedHeader("X-PINGARUNER")
    		      .allowedHeader("Content-Type"));
    	
    	router.route().handler(CookieHandler.create());
        router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
        

    	
    	//login auth API
    	loginUser(router);
    	
    	//register API
    	registerUser(router);
    	 
    	getSessionDetails(router);
    	
    	// submit blog
    	
    	submitBlog(router);
    	
    	getCompany(router);
    	
    	getSites(router);
    	
    	getDept(router);
    	
    	getBlogs(router);
    	
    	searchBlogsWithTags(router);
    	
    	//post API
    	 // postAPI(router);
    	
    	server.requestHandler(router::accept).listen(8084);

    }
    
    public void getSessionDetails(Router router) {
    	
    	router.get("/Services/rest/user").handler(ctx -> {
    			
    		  System.out.println(ctx.currentRoute());
    	      if(this.userName != null) {
    	    	  
    	    	  client.findOne("users", new JsonObject().put("userName", this.userName), null, lookup -> {
    	    	        // error handling
    	    	        if (lookup.failed()) {
    	    	          ctx.fail(500);
    	    	          return;
    	    	        }

    	    	        JsonObject user = lookup.result();

    	    	        if (user == null) {
    	    	          ctx.fail(404);
    	    	        } else {
    	    	          ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
    	    	          ctx.response().end(user.encode());
    	    	        }
    	    	      });
    	      }
    	});
    	
    }
    
    public void submitBlog(Router router) {
    	router.post("/Services/rest/blogs").handler(ctx -> {

		      JsonObject blogDetails = ctx.getBodyAsJson();
		      
		      client.findOne("blogs", new JsonObject().put("title", blogDetails.getString("title"))
		      , null, lookup -> {
		    		    	 
		      // error handling
		      if (lookup.failed()) {
		        ctx.fail(500);
		        return;
		      }
		
		      JsonObject blog = lookup.result();
		
		      if (blog != null) {
			    // already exists
		    	  ctx.fail(500);
	            
		      } else {
		    	  
		    	  
		    	  client.insert("blogs", blogDetails, insert -> {
		              // error handling
		              if (insert.failed()) {
		                ctx.fail(500);
		                return;
		              }

		              // add the generated id to the user object
		              blogDetails.put("_id", insert.result());

		              ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
		              System.out.println(blogDetails.encode());
		              ctx.response().end(blogDetails.encode());
		            });

		      }
    	      
    	    });
    	});
    }
    
    
    public void loginUser(Router router) {
    	router.post("/Services/rest/user/auth").handler(ctx -> {
    	      Session session = ctx.session();

		      JsonObject userDetails = ctx.getBodyAsJson();
		      
		      client.findOne("users", new JsonObject().put("userName", userDetails.getString("userName"))
		      .put("password", userDetails.getString("password")), null, lookup -> {
		    		    	 
		      // error handling
		      if (lookup.failed()) {
		        ctx.fail(500);
		        return;
		      }
		
		      JsonObject user = lookup.result();
		
		      if (user != null) {
			    session.put("userName", userDetails.getString("userName"));
			    
			    this.userName = userDetails.getString("userName");
			    
			    ctx.addCookie(Cookie.cookie("userName", userDetails.getString("userName")));
		    	ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
	            ctx.response().setStatusCode(200);
	            ctx.response().end(userDetails.encode());
	            
		      } else {
		        ctx.fail(404);
		        return;
		      }
    	      
    	    });
    	});
    }
    
    public String insertSites(String companyId, String deptName, RoutingContext ctx) {
    	
    	String defaultSiteName = "London";
    	    	
    	JsonObject sitesObj = new JsonObject().put("companyId", companyId).put("siteName", defaultSiteName);
  	  	client.findOne("site", sitesObj, null, lookup -> {
            // error handling
            if (lookup.failed()) {
              ctx.fail(500);
              return;
            }
            System.out.println("Site Lookup result : " + lookup.result());
            if(lookup.result() != null) {
                // already exists
                // ctx.fail(500);
            } else {
            	JsonObject site = new JsonObject().put("companyId", companyId).put("siteName", defaultSiteName);
            	client.insert("site", site, insert -> {
                    // error handling
                    if (insert.failed()) {
                      ctx.fail(500);
                      return;
                    }
                    System.out.println("Site insert result : " + insert.result());
                    // add the generated id to the dept object
    	            site.put("_id", insert.result());
    	            System.out.println(site.encode());
    	            siteId = insert.result();
                    this.insertDept(companyId, siteId, deptName, ctx);
                  });
            }
          });
  	  	
  	  	return siteId;
    }
    
    public String insertDept(String companyId, String siteId, String deptName, RoutingContext ctx) {
    	
    	
    	JsonObject deptObj = new JsonObject().put("companyId", companyId).put("siteId", siteId);
  	  	client.findOne("dept", deptObj, null, lookup -> {
            // error handling
            if (lookup.failed()) {
              ctx.fail(500);
              return;
            }
            System.out.println("Dept Lookup result : " + lookup.result());
            if(lookup.result() != null) {
                // already exists
                // ctx.fail(500);
            } else {
            	JsonObject dept = new JsonObject().put("companyId", companyId).put("siteId", siteId).put("deptName", deptName);
            	client.insert("dept", dept, insert -> {
                    // error handling
                    if (insert.failed()) {
                      ctx.fail(500);
                      return;
                    }
                    System.out.println("Dept insert result : " + insert.result());
                    // add the generated id to the dept object
    	            dept.put("_id", insert.result());
    	            System.out.println(dept.encode());
    	            deptId = insert.result();
                    
                  });
            }
          });
  	  	  	  	
  	  	return deptId;
    }
    
    public void insertCompany(JsonObject userDetails, String deptName, RoutingContext ctx) {
    	
    	JsonObject companyObj = new JsonObject().put("companyName", userDetails.getString("companyName")).put("subdomain", userDetails.getString("subdomain"));
  	  	client.findOne("company", companyObj, null, lookup -> {
            // error handling
            if (lookup.failed()) {
              ctx.fail(500);
              return;
            }
            
            System.out.println("Company Lookup result : " + lookup.result());
            
            if(lookup.result() != null) {
            	System.out.println(lookup.result());
                // already exists
                // ctx.fail(500);
            } else {
            	JsonObject company = new JsonObject().put("companyName", userDetails.getString("companyName")).put("subdomain", userDetails.getString("subdomain"));
            	client.insert("company", company, insert -> {
                    // error handling
                    if (insert.failed()) {
                      ctx.fail(500);
                      return;
                    }
                    System.out.println("Company insert result : " + insert.result());
                    // add the generated id to the dept object
    	            company.put("_id", insert.result());
    	            companyId = insert.result();
    	            this.insertSites(companyId, deptName, ctx);
                  });
            }
          });
  	  	
  	  	// return companyId;
    }
    
    public void registerUser(Router router) {
    	router.post("/Services/rest/user/register").handler(ctx -> {

		      JsonObject userDetails = ctx.getBodyAsJson();
		      Boolean isCompany = userDetails.getBoolean("isCompany");
		     
		     		      
		      if( userDetails.containsKey("isCompany") && isCompany) {
		    	  
		    	  
		    	  vertx.executeBlocking(future -> {
		    		  // Call some blocking API that takes a significant amount of time to return
		    		  this.insertCompany(userDetails, userDetails.getString("deptName"), ctx);
		    		  future.complete();
		    		}, res -> {
		    		  System.out.println("The result is: " + res.result());
		    		});

		      }
		      
		      client.findOne("users", new JsonObject().put("userName", userDetails.getString("userName"))
		      .put("email", userDetails.getString("email")), null, lookup -> {
		    		    	 
		      // error handling
		      if (lookup.failed()) {
		        ctx.fail(500);
		        return;
		      }

		      JsonObject user = lookup.result();
		
		      if (user != null) {
		    	  // already exists
		    	  ctx.fail(500);
		      } else {
		    	  if( userDetails.containsKey("isCompany") && isCompany){
		    	  userObj = new JsonObject().put("userName", userDetails.getString("userName")).put("password", userDetails.getString("password"))
		    			  .put("email", userDetails.getString("email")).put("first", userDetails.getString("first"))
		    			  .put("last", userDetails.getString("last")).put("companyId", cId).put("siteId", sId)
		    			  .put("deptId", dId);
		    	  } else {
		    		  
		    		  userObj = new JsonObject().put("userName", userDetails.getString("userName")).put("password", userDetails.getString("password"))
			    			  .put("email", userDetails.getString("email")).put("first", userDetails.getString("first"))
			    			  .put("last", userDetails.getString("last")).put("companyId", userDetails.getString("companyId")).put("siteId", userDetails.getString("siteId"))
			    			  .put("deptId", userDetails.getString("deptId"));
		    	  }
		    	  client.insert("users", userObj, insert -> {
		              // error handling
		              if (insert.failed()) {
		                ctx.fail(500);
		                return;
		              }
		              this.userName = userDetails.getString("userName");
		              
		              
		              // add the generated id to the user object
		              userObj.put("id", insert.result());

		              ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
		              ctx.response().end(userObj.encode());
		            });

		        return;
		      }
    	      
    	    });
    	});
    }
    

	public void getCompany(Router router) {
    	router.get("/Services/rest/company").handler(ctx -> {
    		

    		client.find("company", new JsonObject(), lookup -> {
    	        // error handling
    	        if (lookup.failed()) {
    	          ctx.fail(500);
    	          return;
    	        }

    	        List<JsonObject> company = lookup.result();

    	        if (company == null) {
    	          ctx.fail(404);
    	          
    	        } else {
    	        	
    	        	System.out.println(company.toString());
    	        	
    	        	ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
		            ctx.response().end(company.toString());
    	        }
    	      });
    	});
    }
    
    public void getSites(Router router) {
    	router.get("/Services/rest/company/:id/sites").handler(ctx -> {
    		
    		client.find("site", new JsonObject().put("companyId", ctx.request().getParam("id")), lookup -> {
    	        // error handling
    	        if (lookup.failed()) {
    	          ctx.fail(500);
    	          return;
    	        }

    	        List<JsonObject> site = lookup.result();

    	        if (site == null) {
    	          ctx.fail(404);
    	        } else {
    	        	
    	        	ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
		            ctx.response().end(site.toString());
    	        }
    	      });
    	});
    }
    
    
    public void getDept(Router router) {
    	router.get("/Services/rest/company/:id/sites/:siteid/departments").handler(ctx -> {
    		
    		client.find("dept", new JsonObject().put("siteId", ctx.request().getParam("siteid")), lookup -> {
    	        // error handling
    	        if (lookup.failed()) {
    	          ctx.fail(500);
    	          return;
    	        }

    	        List<JsonObject> dept = lookup.result();

    	        if (dept == null) {
    	          ctx.fail(404);
    	        } else {
    	        	
    	        	ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
		            ctx.response().end(dept.toString());
    	        }
    	      });
    	});
    }
    
    public void getBlogs(Router router) {
    	router.get("/Services/rest/blogs").handler(ctx -> {
    		
    		client.find("blogs", new JsonObject(), lookup -> {
    	        // error handling
    	        if (lookup.failed()) {
    	          ctx.fail(500);
    	          return;
    	        }

    	        List<JsonObject> blogs = lookup.result();

    	        if (blogs == null) {
    	          ctx.fail(404);
    	        } else {
    	        	
    	        	ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
		            ctx.response().end(blogs.toString());
    	        }
    	      });
    	});
    }
    
    
    public void searchBlogsWithTags(Router router) {
    	router.get("/Services/rest/blogs/:tags").handler(ctx -> {
    		
    		client.find("blogs", new JsonObject().put("tags", ctx.request().getParam("tags")), lookup -> {
    	        // error handling
    	        if (lookup.failed()) {
    	          ctx.fail(500);
    	          return;
    	        }

    	        List<JsonObject> blogs = lookup.result();

    	        if (blogs == null) {
    	          ctx.fail(404);
    	        } else {
    	        	
    	        	ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
		            ctx.response().end(blogs.toString());
    	        }
    	      });
    	});
    }
    
    public static void main( String[] args )
    {	
    	VertxOptions options = new VertxOptions().setWorkerPoolSize(10);
    	Vertx vertx = Vertx.vertx(options);
    	
    	vertx.deployVerticle("com.cisco.cmad.vertx_backend.App", new Handler<AsyncResult<String>>() {

			@Override
			public void handle(AsyncResult<String> stringAsyncResult) {
				System.out.println("Verticle Deployment completed");
				
			}
    		
    	});
    	
    }
}
