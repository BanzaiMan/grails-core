/* Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.test

import org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.codehaus.groovy.grails.webflow.engine.builder.FlowBuilder

import org.springframework.binding.convert.service.DefaultConversionService
import org.springframework.context.ApplicationContext
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.support.WebApplicationContextUtils
import org.springframework.web.servlet.ViewResolver
import org.springframework.webflow.context.ExternalContext
import org.springframework.webflow.context.servlet.ServletExternalContext
import org.springframework.webflow.definition.FlowDefinition
import org.springframework.webflow.definition.registry.FlowDefinitionRegistryImpl
import org.springframework.webflow.engine.builder.support.FlowBuilderServices
import org.springframework.webflow.expression.DefaultExpressionParserFactory
import org.springframework.webflow.mvc.builder.MvcViewFactoryCreator
import org.springframework.webflow.test.MockExternalContext
import org.springframework.webflow.test.execution.AbstractFlowExecutionTests
import org.springframework.webflow.definition.registry.FlowDefinitionRegistry
import org.springframework.webflow.engine.builder.FlowAssembler
import org.springframework.webflow.engine.builder.DefaultFlowHolder
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes

/**
 * A test harness for testing Grails flows.
 */
abstract class WebFlowTestCase extends AbstractFlowExecutionTests {

    protected MockHttpServletRequest mockRequest
    protected MockHttpServletResponse mockResponse
    protected MockServletContext mockServletContext
    protected ApplicationContext applicationContext
    /**
     * Subclasses should return the flow closure that within the controller. For example:
     * <code>return new TestController().myFlow</code>.
     */
    abstract getFlow()

    /**
     * Subclasses should override to change flow id.
     */
    String getFlowId() { "test" }

    protected void setUp() {
        super.setUp()
        GrailsWebRequest webRequest = RequestContextHolder.getRequestAttributes()

        if (webRequest) {
            mockRequest = webRequest.currentRequest
            mockResponse = webRequest.currentResponse
            mockServletContext = webRequest.getServletContext()
            applicationContext = WebApplicationContextUtils.getWebApplicationContext(webRequest.getServletContext())
        }
        else {
            applicationContext = new GrailsWebApplicationContext()
            mockRequest = new MockHttpServletRequest()
            mockResponse = new MockHttpServletResponse()
            mockServletContext = new MockServletContext()
            RequestContextHolder.setRequestAttributes(new GrailsWebRequest(mockRequest,mockResponse,mockServletContext,applicationContext))
        }

    }

    protected void tearDown() {
        super.tearDown()
        mockRequest = null
        mockResponse = null
        mockServletContext = null
        RequestContextHolder.setRequestAttributes(null)
    }


    FlowDefinition getFlowDefinition() {
        def flowBuilderServices = new FlowBuilderServices()

        MvcViewFactoryCreator viewCreator = new MvcViewFactoryCreator()
        viewCreator.applicationContext = applicationContext
        def viewResolvers = applicationContext?.getBeansOfType(ViewResolver)
        if (viewResolvers) {
            viewCreator.viewResolvers = viewResolvers.values().toList()
        }
        flowBuilderServices.viewFactoryCreator = viewCreator
        flowBuilderServices.conversionService = new DefaultConversionService()
        flowBuilderServices.expressionParser = DefaultExpressionParserFactory.getExpressionParser()

        FlowBuilder builder = new FlowBuilder(getFlowId(), flowBuilderServices, new FlowDefinitionRegistryImpl())
        def flow = getFlow()

        if(flow instanceof Closure) {
            // delegate will be controller
            GrailsWebRequest.lookup()?.request?.setAttribute(GrailsApplicationAttributes.CONTROLLER, flow.delegate)
            builder.flow(flow)
        }
    }

    /**
     * Initiates a web flow.
     */
    protected void startFlow() {
        super.startFlow(new ServletExternalContext(mockServletContext, mockRequest, mockResponse))
    }

    /**
     * Triggers a web flow event for the given eventId and returns the ExternalContext
     * used to trigger the event.
     */
    protected ExternalContext signalEvent(String eventId) {
        MockExternalContext context = new MockExternalContext()
        context.setNativeRequest mockRequest
        context.setNativeResponse mockResponse
        context.setNativeContext mockServletContext
        context.setEventId(eventId)
        resumeFlow(context)
        return context
    }
}
