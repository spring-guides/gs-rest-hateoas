<#assign project_id="gs-rest-hateoas">

This guide walks you through the process of creating a "hello world" [Hypermedia Driven REST web service][u-rest] with Spring.

[Hypermedia][wikipedia-hateoas] is an important aspect of [REST][u-rest]. It allows you to build services that decouple client and server to a large extent and allow them to evolve independently. The representations returned for REST resources contain links that indicate which further resources the client should look at and interact with. Thus the design of the representations is crucial to the design of the overall service. 

What you'll build
-----------------

You'll build a hypermedia-driven REST service with Spring HATEOAS, a library of APIs that you can use to easily create links pointing to Spring MVC controllers, build up resource representations, and control how they're rendered into supported hypermedia formats such as HAL.

The service will accept HTTP GET requests at:

    http://localhost:8080/greeting

and respond with a [JSON][u-json] representation of a greeting enriched with the simplest possible hypermedia element, a link pointing to the resource itself:

    { "links" : [ { "rel" : "self",
                    "href" : "http://localhost:8080/greeting?name=World" } ],    
      "content" : "Hello, World!" }

The response already indicates you can customize the greeting with an optional `name` parameter in the query string:

    http://localhost:8080/greeting?name=User

The `name` parameter value overrides the default value of "World" and is reflected in the response:

    { "links" : [ { "rel" : "self",
                    "href" : "http://localhost:8080/greeting?name=User" } ],    
      "content" : "Hello, User!" }


What you'll need
----------------

 - About 15 minutes
 - <@prereq_editor_jdk_buildtools/>

## <@how_to_complete_this_guide jump_ahead='Create a resource representation class'/>


<a name="scratch"></a>
Set up the project
------------------

<@build_system_intro/>

<@create_directory_structure_hello/>

<@create_both_builds/>

<@bootstrap_starter_pom_disclaimer/>

<a name="initial"></a>
Create a resource representation class
--------------------------------------

Now that you've set up the project and build system, you can create your web service.

Begin the process by thinking about service interactions.

The service will expose a resource at `/greeting` to handle `GET` requests, optionally with a `name` parameter in the query string. The `GET` request should return a `200 OK` response with JSON in the body that represents a greeting. 

Beyond that, the JSON representation of the resource will be enriched with a list of hypermedia elements in a `links` property. The most rudimentary form of this is a link pointing to the resource itself. So the representation should look something like this:

    { "links" : [ { "rel" : "self",
                    "href" : "http://localhost:8080/greeting?name=World" } ],    
      "content" : "Hello, World!" }

The `content` is the textual representation of the greeting. The `links` element contains a list of links, in this case exactly one with the relation type of `rel` and the `href` attribute pointing to the resource just accessed.

To model the greeting representation, you create a resource representation class. 
As the `links` property is a fundamental property of the representation model, Spring HATEOAS ships with a base class `ResourceSupport` that allows you to add instances of `Link` and ensures that they are rendered as shown above.

So you simply create a plain old java object extending `ResourceSupport` and add the field and accessor for the content as well as a constructor:

    <@snippet path="src/main/java/hello/Greeting.java" prefix="complete"/>


> **Note:** As you'll see in steps below, Spring will use the _Jackson_ JSON library to automatically marshal instances of type `Greeting` into JSON.

Next you create the resource controller that will serve these greetings.


Create a resource controller
------------------------------

In Spring's approach to building RESTful web services, HTTP requests are handled by a controller. The components are easily identified by the [`@Controller`][] annotation, and the `GreetingController` below handles `GET` requests for `/greeting` by returning a new instance of the `Greeting` class:

    <@snippet path="src/main/java/hello/GreetingController.java" prefix="complete"/>

This controller is concise and simple, but there's plenty going on. Let's break it down step by step.

The `@RequestMapping` annotation ensures that HTTP requests to `/greeting` are mapped to the `greeting()` method.

> **Note:** The above example does not specify `GET` vs. `PUT`, `POST`, and so forth, because `@RequestMapping` maps _all_ HTTP operations by default. Use `@RequestMapping(method=GET)` to narrow this mapping.

`@RequestParam` binds the value of the query string parameter `name` into the `name` parameter of the `greeting()` method. This query string parameter is not `required`; if it is absent in the request, the `defaultValue` of "World" is used.

The [`@ResponseBody`][] annotation on the `greeting` method will cause Spring MVC to render the returned `HttpEntity` and its payload, the `Greeting`, directly to the response.

The most interesting part of the method implementation is how you create the link pointing to the controller method and how you add it to the representation model. Both `linkTo(…)` and `methodOn(…)` are static methods on `ControllerLinkBuilder` that allow you to fake a method invocation on the controller. The `LinkBuilder` returned will have inspected the controller method's mapping annotation to build up exactly the URI the method is mapped to.

The call to `withSelfRel()` creates a `Link` instance that you add to the `Greeting` representation model.


Make the application executable
-------------------------------

Although it is possible to package this service as a traditional _web application archive_ or [WAR][u-war] file for deployment to an external application server, the simpler approach demonstrated below creates a _standalone application_. You package everything in a single, executable JAR file, driven by a good old Java `main()` method. And along the way, you use Spring's support for embedding the [Tomcat][u-tomcat] servlet container as the HTTP runtime, instead of deploying to an external instance.

### Create an Application class

    <@snippet path="src/main/java/hello/Application.java" prefix="complete"/>

The `main()` method defers to the [`SpringApplication`][] helper class, providing `Application.class` as an argument to its `run()` method. This tells Spring to read the annotation metadata from `Application` and to manage it as a component in the _[Spring application context][u-application-context]_.

The `@ComponentScan` annotation tells Spring to search recursively through the `hello` package and its children for classes marked directly or indirectly with Spring's [`@Component`][] annotation. This directive ensures that Spring finds and registers the `GreetingController`, because it is marked with `@Controller`, which in turn is a kind of `@Component` annotation.

The [`@EnableAutoConfiguration`][] annotation switches on reasonable default behaviors based on the content of your classpath. For example, because the application depends on the embeddable version of Tomcat (tomcat-embed-core.jar), a Tomcat server is set up and configured with reasonable defaults on your behalf. And because the application also depends on Spring MVC (spring-webmvc.jar), a Spring MVC [`DispatcherServlet`][] is configured and registered for you — no `web.xml` necessary! Auto-configuration is a powerful, flexible mechanism. See the [API documentation][`@EnableAutoConfiguration`] for further details.

<@build_an_executable_jar_subhead/>
<@build_an_executable_jar_with_both/>

<@run_the_application_with_both module="service"/>

Logging output is displayed. The service should be up and running within a few seconds.


Test the service
----------------

Now that the service is up, visit <http://localhost:8080/greeting>, where you see:

    { "links" : [ { "rel" : "self",
                    "href" : "http://localhost:8080/greeting?name=World" } ],    
      "content" : "Hello, World!" }

Provide a `name` query string parameter with <http://localhost:8080/greeting?name=User>. Notice how the value of the `content` attribute changes from "Hello, World!" to "Hello User!" and the `href` attribute of the `self` link reflects that change as well:

    { "links" : [ { "rel" : "self",
                    "href" : "http://localhost:8080/greeting?name=User" } ],    
      "content" : "Hello, User!" }

This change demonstrates that the `@RequestParam` arrangement in `GreetingController` is working as expected. The `name` parameter has been given a default value of "World", but can always be explicitly overridden through the query string.

Summary
-------

Congratulations! You've just developed a hypermedia-driven RESTful web service with Spring HATEOAS. 

[wikipedia-hateoas]: http://en.wikipedia.org/wiki/HATEOAS
<@u_rest/>
<@u_json/>
[jackson]: http://wiki.fasterxml.com/JacksonHome
<@u_war/>
<@u_tomcat/>
<@u_application_context/>
[`@Controller`]: http://static.springsource.org/spring/docs/current/javadoc-api/org/springframework/stereotype/Controller.html
[`SpringApplication`]: http://static.springsource.org/spring-bootstrap/docs/0.5.0.M2/javadoc-api/org/springframework/bootstrap/SpringApplication.html
[`@EnableAutoConfiguration`]: http://static.springsource.org/spring-bootstrap/docs/0.5.0.M2/javadoc-api/org/springframework/bootstrap/context/annotation/SpringApplication.html
[`@Component`]: http://static.springsource.org/spring/docs/current/javadoc-api/org/springframework/stereotype/Component.html
[`@ResponseBody`]: http://static.springsource.org/spring/docs/current/javadoc-api/org/springframework/web/bind/annotation/ResponseBody.html
[`MappingJackson2HttpMessageConverter`]: http://static.springsource.org/spring/docs/current/javadoc-api/org/springframework/http/converter/json/MappingJackson2HttpMessageConverter.html
[`DispatcherServlet`]: http://static.springsource.org/spring/docs/current/javadoc-api/org/springframework/web/servlet/DispatcherServlet.html
