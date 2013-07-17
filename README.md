
# Getting Started: Building a Hypermedia Driven REST Web Service

An important aspect of [REST][u-rest] is the [hypermedia][wikipedia-hateoas] one. It allows you to build services that decouple client and server to a large extend and thus allow them to be evolved independently. To achieve this, the representations returned for REST resources contain links that indicate which further resources the client should look at and interact with.

As a consequence, the design of the representations becomes an important aspect of the design of the overall service. Spring HATEOAS is a library that helps you achieving that by providing APIs to easily create links pointing to Spring MVC controllers, build up resource representations and control how they're rendered into various supported hypermedia formats such as HAL etc.

What you'll build
-----------------

This guide walks you through creating a "hello world" [Hypermedia Driven REST web service][u-rest] with Spring. The service will accept HTTP GET requests at:

    http://localhost:8080/greeting

and respond with a [JSON][u-json] representation of a greeting enriched with the simplest possible hypermedia element, a link pointing to the resource itself:

    { "links" : [ { "rel" : "self",
                    "href" : "http://localhost:8080/greeting?name=World" } ],    
      "content" : "Hello, World!" }

As the response already indicates you can customize the greeting with an optional `name` parameter in the query string:

    http://localhost:8080/greeting?name=User

The `name` parameter value overrides the default value of "World" and is reflected in the response:

    { "links" : [ { "rel" : "self",
                    "href" : "http://localhost:8080/greeting?name=User" } ],    
      "content" : "Hello, User!" }


What you'll need
----------------

 - About 15 minutes
 - A favorite text editor or IDE
 - [JDK 6][jdk] or later
 - [Maven 3.0][mvn] or later

[jdk]: http://www.oracle.com/technetwork/java/javase/downloads/index.html
[mvn]: http://maven.apache.org/download.cgi

How to complete this guide
--------------------------

Like all Spring's [Getting Started guides](/getting-started), you can start from scratch and complete each step, or you can bypass basic setup steps that are already familiar to you. Either way, you end up with working code.

To **start from scratch**, move on to [Set up the project](#scratch).

To **skip the basics**, do the following:

 - [Download][zip] and unzip the source repository for this guide, or clone it using [git](/understanding/git):
`git clone https://github.com/springframework-meta/gs-rest-hateoas.git`
 - cd into `gs-rest-hateoas/initial`
 - Jump ahead to [Create a resource representation class](#initial).

**When you're finished**, you can check your results against the code in `gs-rest-hateoas/complete`.
[zip]: https://github.com/springframework-meta/gs-rest-hateoas/archive/master.zip


<a name="scratch"></a>
Set up the project
------------------

First you set up a basic build script. You can use any build system you like when building apps with Spring, but the code you need to work with [Maven](https://maven.apache.org) and [Gradle](http://gradle.org) is included here. If you're not familiar with either, refer to [Building Java Projects with Maven](../gs-maven/README.md) or [Building Java Projects with Gradle](../gs-gradle/README.md).

### Create the directory structure

In a project directory of your choosing, create the following subdirectory structure; for example, with `mkdir -p src/main/java/hello` on *nix systems:

    └── src
        └── main
            └── java
                └── hello

### Create a Maven POM

`pom.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>org.springframework</groupId>
    <artifactId>gs-rest-hateoas-complete</artifactId>
    <version>0.1.0</version>

    <parent>
        <groupId>org.springframework.bootstrap</groupId>
        <artifactId>spring-bootstrap-starters</artifactId>
        <version>0.5.0.BUILD-SNAPSHOT</version>
    </parent>

    <dependencies>
        <dependency>
            <groupId>org.springframework.bootstrap</groupId>
            <artifactId>spring-bootstrap-web-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.hateoas</groupId>
            <artifactId>spring-hateoas</artifactId>
        </dependency>
    </dependencies>

    <properties>
        <start-class>hello.Application</start-class>
    </properties>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

    <!-- TODO: remove once bootstrap goes GA -->
    <repositories>
        <repository>
            <id>spring-snapshots</id>
            <url>http://repo.springsource.org/snapshot</url>
            <snapshots><enabled>true</enabled></snapshots>
        </repository>
    </repositories>
    <pluginRepositories>
        <pluginRepository>
            <id>spring-snapshots</id>
            <url>http://repo.springsource.org/snapshot</url>
            <snapshots><enabled>true</enabled></snapshots>
        </pluginRepository>
    </pluginRepositories>
</project>
```

TODO: mention that we're using Spring Bootstrap's [_starter POMs_](../gs-bootstrap-starter) here.

Note to experienced Maven users who are unaccustomed to using an external parent project: you can take it out later, it's just there to reduce the amount of code you have to write to get started.

<a name="initial"></a>
Create a resource representation class
--------------------------------------

Now that you've set up the project and build system, you can create your web service.

Begin the process by thinking about service interactions.

The service will expose a resource at `/greeting` to handle `GET` requests, optionally with a `name` parameter in the query string. The `GET` request should return a `200 OK` response with JSON in the body that represents a greeting. 

Beyond that the JSON representation of the resource shall be enriched with a list of hypermedia elements in a `links` property. The most rudimentary form of this is a link pointing to the resource itself. So the representation should look something like this:

    { "links" : [ { "rel" : "self",
                    "href" : "http://localhost:8080/greeting?name=World" } ],    
      "content" : "Hello, World!" }

The `content` is the textual representation of the greeting. The `links` element contains a list of links, in our case exactly one with the relation type of `rel` and the `href` attribute pointing to the resource just accessed.

To model the greeting representation, you create a _resource representation class_. 
As the `links` property is a fundamental property of the representation model Spring HATEOAS ships with a base class `ResourceSupport` that allows adding instances of `Link` and makes sure they're getting rendered as shown above.

So you simply create a plain old java object extending `ResourceSupport` and add the field and accessor for the content as well as a constructor:

`src/main/java/hello/Greeting.java`
```java
package hello;

import org.springframework.hateoas.ResourceSupport;

public class Greeting extends ResourceSupport {

	private final String content;

	public Greeting(String content) {
		this.content = content;
	}

	public String getContent() {
		return content;
	}
}
```


> **Note:** As you'll see in steps below, Spring will use the _Jackson_ JSON library to automatically marshal instances of type `Greeting` into JSON.

Next you create the resource controller that will serve these greetings.


Create a resource controller
------------------------------

In Spring's approach to building RESTful web services, HTTP requests are handled by a _controller_. These components are easily identified by the [`@Controller`][] annotation, and the `GreetingController` below handles `GET` requests for `/greeting` by returning a new instance of the `Greeting` class:

`src/main/java/hello/GreetingController.java`
```java
package hello;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.*;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class GreetingController {

	private static final String TEMPLATE = "Hello, %s!";

	@RequestMapping("/greeting")
	@ResponseBody
	public HttpEntity<Greeting> greeting(
			@RequestParam(value = "name", required = false, defaultValue = "World") String name) {

		Greeting greeting = new Greeting(String.format(TEMPLATE, name));
		greeting.add(linkTo(methodOn(GreetingController.class).greeting(name)).withSelfRel());

		return new ResponseEntity<Greeting>(greeting, HttpStatus.OK);
	}
}
```

This controller is concise and simple, but there's plenty going on under the hood. Let's break it down step by step.

The `@RequestMapping` annotation ensures that HTTP requests to `/greeting` are mapped to the `greeting()` method.

> **Note:** The above example does not specify `GET` vs. `PUT`, `POST`, and so forth, because `@RequestMapping` maps _all_ HTTP operations by default. Use `@RequestMapping(method=GET)` to narrow this mapping.

`@RequestParam` binds the value of the query string parameter `name` into the `name` parameter of the `greeting()` method. This query string parameter is not `required`; if it is absent in the request, the `defaultValue` of "World" is used.

The [`@ResponseBody`][] annotation on the method will cause Spring MVC to render the returned `HttpEntity` and its payload, the `Greeting`, directly to the response.

The most interesting part of the method implementation is how we create the link pointing to the controller method and how we add it to the representation model. Both `linkTo(…)` and `methodOn(…)` are static methods on `ControllerLinkBuilder` that allow you to fake a method invocation on the controller. The `LinkBuilder` returned will have inspected the controller method's mapping annotation to build up exactly the URI the method is mapped to.

The call to `withSelfRel()` finally creates a `Link` instances we add to the `Greeting` representation model.


Make the application executable
-------------------------------

Although it is possible to package this service as a traditional _web application archive_ or [WAR][u-war] file for deployment to an external application server, the simpler approach demonstrated below creates a _standalone application_. You package everything in a single, executable JAR file, driven by a good old Java `main()` method. And along the way, you use Spring's support for embedding the [Tomcat][u-tomcat] servlet container as the HTTP runtime, instead of deploying to an external instance.

### Create a main class

`src/main/java/hello/Application.java`
```java
package hello;

import org.springframework.bootstrap.SpringApplication;
import org.springframework.bootstrap.context.annotation.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan
@EnableAutoConfiguration
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

The `main()` method defers to the [`SpringApplication`][] helper class, providing `Application.class` as an argument to its `run()` method. This tells Spring to read the annotation metadata from `Application` and to manage it as a component in the _[Spring application context][u-application-context]_.

The `@ComponentScan` annotation tells Spring to search recursively through the `hello` package and its children for classes marked directly or indirectly with Spring's [`@Component`][] annotation. This directive ensures that Spring finds and registers the `GreetingController`, because it is marked with `@Controller`, which in turn is a kind of `@Component` annotation.

The [`@EnableAutoConfiguration`][] annotation switches on reasonable default behaviors based on the content of your classpath. For example, because the application depends on the embeddable version of Tomcat (tomcat-embed-core.jar), a Tomcat server is set up and configured with reasonable defaults on your behalf. And because the application also depends on Spring MVC (spring-webmvc.jar), a Spring MVC [`DispatcherServlet`][] is configured and registered for you — no `web.xml` necessary! Auto-configuration is a powerful, flexible mechanism. See the [API documentation][`@EnableAutoConfiguration`] for further details.

Now that your `Application` class is ready, you simply instruct the build system to create a single, executable jar containing everything. This makes it easy to ship, version, and deploy the service as an application throughout the development lifecycle, across different environments, and so forth.

Add the following configuration to your existing Maven POM:

`pom.xml`
```xml
    <properties>
        <start-class>hello.Application</start-class>
    </properties>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
```

The `start-class` property tells Maven to create a `META-INF/MANIFEST.MF` file with a `Main-Class: hello.Application` entry. This entry enables you to run the jar with `java -jar`.

The [Maven Shade plugin][maven-shade-plugin] extracts classes from all jars on the classpath and builds a single "über-jar", which makes it more convenient to execute and transport your service.

Now run the following to produce a single executable JAR file containing all necessary dependency classes and resources:

    mvn package

[maven-shade-plugin]: https://maven.apache.org/plugins/maven-shade-plugin

> **Note:** The procedure above will create a runnable JAR. You can also opt to [build a classic WAR file](/guides/gs/convert-jar-to-war/content) instead.

Run the service
-------------------
Run your service with `java -jar` at the command line:

    java -jar target/gs-rest-hateoas-0.1.0.jar



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

Congrats! You've just developed a hypermedia driven REST web service with Spring. This of course is just the beginning, and there are many more features to explore and take advantage of. Be sure to check out Spring's support for [securing](TODO), [describing](TODO) [managing](TODO), [testing](TODO) and [consuming](/gs-consuming-rest) RESTful web services.

[wikipedia-hateoas]: http://en.wikipedia.org/wiki/HATEOAS
[u-rest]: /understanding/rest
[u-json]: /understanding/json
[u-jsp]: /understanding/jsp
[jackson]: http://wiki.fasterxml.com/JacksonHome
[u-war]: /understanding/war
[u-tomcat]: /understanding/tomcat
[u-application-context]: /understanding/application-context
[`@Controller`]: http://static.springsource.org/spring/docs/current/javadoc-api/org/springframework/stereotype/Controller.html
[`SpringApplication`]: http://static.springsource.org/spring-bootstrap/docs/0.5.0.BUILD-SNAPSHOT/javadoc-api/org/springframework/bootstrap/SpringApplication.html
[`@EnableAutoConfiguration`]: http://static.springsource.org/spring-bootstrap/docs/0.5.0.BUILD-SNAPSHOT/javadoc-api/org/springframework/bootstrap/context/annotation/SpringApplication.html
[`@Component`]: http://static.springsource.org/spring/docs/current/javadoc-api/org/springframework/stereotype/Component.html
[`@ResponseBody`]: http://static.springsource.org/spring/docs/current/javadoc-api/org/springframework/web/bind/annotation/ResponseBody.html
[`MappingJackson2HttpMessageConverter`]: http://static.springsource.org/spring/docs/current/javadoc-api/org/springframework/http/converter/json/MappingJackson2HttpMessageConverter.html
[`DispatcherServlet`]: http://static.springsource.org/spring/docs/current/javadoc-api/org/springframework/web/servlet/DispatcherServlet.html
