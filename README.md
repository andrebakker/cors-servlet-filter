CORS Header Scrutiny Filter
===========================

This project provides a CORS ([Cross-Origin Resource Sharing](https://en.wikipedia.org/wiki/Cross-origin_resource_sharing)) HTTP servlet filter to guard against XSRF ([Cross-Site Request Forgery](https://www.owasp.org/index.php/Cross-Site_Request_Forgery_%28CSRF%29)).
The approach taken here is based on [CORS Origin Header Scrutiny (OWASP)](https://www.owasp.org/index.php/CORS_OriginHeaderScrutiny).

How It Works
============

The `CorsHeaderScrutinyServletFilter` inspects the HTTP headers to guard against XSRF.  The following HTTP headers are scrutinized:

* Host
* X-Forwarded-Host
* Origin
* Referer

If provided, the hostname of the Origin and Referer headers must match the hostname provided in the X-Forwarded-Host and Host headers.
This ensures that JavaScript invoking HTTP requests must originate from the same host as the web application.

The approach taken here differs from the [OWASP Countermeasure option B](https://www.owasp.org/index.php/Cross-Site_Request_Forgery_%28CSRF%29) by requiring zero configuration, i.e. there is no need to provide a list of allowed hosts. 

How to Use
==========

Add the dependency to your pom.xml:

````
    <dependency>
      <groupId>com.tasktop</groupId>
      <artifactId>cors-servlet-filter</artifactId>
      <version>1.0.0</version>
    </dependency>
````

In the web.xml of your Java web application:

````
	<filter>
		<filter-name>CORSFilter</filter-name>
		<filter-class>com.tasktop.servlet.cors.CorsHeaderScrutinyServletFilter</filter-class>
	</filter>
	<filter-mapping>
		<filter-name>CORSFilter</filter-name>
		<url-pattern>/*</url-pattern>
	</filter-mapping>
````

This filter should be added at the top of the web.xml so that it is invoked before other filters.

You may also whitelist the endpoints to exclude from the CORS filter by adding the param-name **path-exclusion-pattern**
The values are a list of comma or whitespace separated values for prefixing endpoints to exclude.
e.g. /api/ will exclude /api/some/endpoint/here from the cors filter. It is recommended to have the both beginning and trailing / in your path exclusion pattern.

````
	<filter>
		<filter-name>CORSFilter</filter-name>
		<filter-class>com.tasktop.servlet.cors.CorsHeaderScrutinyServletFilter</filter-class>
		<init-param>
		  <param-name>path-exclusion-pattern</param-name>
		  <param-value>/api/</param-value>
		</init-param>
	</filter>
	<filter-mapping>
		<filter-name>CORSFilter</filter-name>
		<url-pattern>/*</url-pattern>
	</filter-mapping>
````


Building
========

From the command-line:

`mvn clean package`

Build status on [Travis CI](https://travis-ci.org/Tasktop/cors-servlet-filter): ![CI status](https://travis-ci.org/Tasktop/cors-servlet-filter.svg?branch=master "CI Status") 

How To Release
--------------

From the command-line:

````
mvn -Possrh -Psign -DpushChanges=false -DlocalCheckout=true '-Darguments=-Dgpg.passphrase=thesecret -Dgpg.keyname=keyname' release:clean release:prepare
mvn -Possrh -Psign -DpushChanges=false -DlocalCheckout=true '-Darguments=-Dgpg.passphrase=thesecret -Dgpg.keyname=keyname' release:perform
````

Then push changes:

````
git push
````

Search for the staging repository, close and relase it: https://oss.sonatype.org/#stagingRepositories

License
=======

Copyright (c) 2017 Tasktop Technologies

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
