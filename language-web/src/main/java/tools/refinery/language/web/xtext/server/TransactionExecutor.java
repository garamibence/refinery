package tools.refinery.language.web.xtext.server;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.util.IDisposable;
import org.eclipse.xtext.web.server.IServiceContext;
import org.eclipse.xtext.web.server.IServiceResult;
import org.eclipse.xtext.web.server.ISession;
import org.eclipse.xtext.web.server.InvalidRequestException;
import org.eclipse.xtext.web.server.InvalidRequestException.UnknownLanguageException;
import org.eclipse.xtext.web.server.XtextServiceDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.inject.Injector;

import tools.refinery.language.web.xtext.server.message.XtextWebErrorKind;
import tools.refinery.language.web.xtext.server.message.XtextWebErrorResponse;
import tools.refinery.language.web.xtext.server.message.XtextWebOkResponse;
import tools.refinery.language.web.xtext.server.message.XtextWebPushMessage;
import tools.refinery.language.web.xtext.server.message.XtextWebRequest;
import tools.refinery.language.web.xtext.server.push.PrecomputationListener;
import tools.refinery.language.web.xtext.server.push.PushWebDocument;
import tools.refinery.language.web.xtext.servlet.SimpleServiceContext;

public class TransactionExecutor implements IDisposable, PrecomputationListener {
	private static final Logger LOG = LoggerFactory.getLogger(TransactionExecutor.class);

	private final ISession session;

	private final IResourceServiceProvider.Registry resourceServiceProviderRegistry;

	private final Map<String, WeakReference<PushWebDocument>> subscriptions = new HashMap<>();

	private ResponseHandler responseHandler;

	private Object callPendingLock = new Object();

	private boolean callPending;

	private List<XtextWebPushMessage> pendingPushMessages = new ArrayList<>();

	public TransactionExecutor(ISession session, IResourceServiceProvider.Registry resourceServiceProviderRegistry) {
		this.session = session;
		this.resourceServiceProviderRegistry = resourceServiceProviderRegistry;
	}

	public void setResponseHandler(ResponseHandler responseHandler) {
		this.responseHandler = responseHandler;
	}

	public void handleRequest(XtextWebRequest request) throws ResponseHandlerException {
		var serviceContext = new SimpleServiceContext(session, request.getRequestData());
		var ping = serviceContext.getParameter("ping");
		if (ping != null) {
			responseHandler.onResponse(new XtextWebOkResponse(request, new PongResult(ping)));
			return;
		}
		synchronized (callPendingLock) {
			if (callPending) {
				LOG.error("Reentrant request detected");
			}
			if (!pendingPushMessages.isEmpty()) {
				LOG.error("{} push messages got stuck without a pending request", pendingPushMessages.size());
			}
			callPending = true;
		}
		try {
			var injector = getInjector(serviceContext);
			var serviceDispatcher = injector.getInstance(XtextServiceDispatcher.class);
			var service = serviceDispatcher.getService(new SubscribingServiceContext(serviceContext, this));
			var serviceResult = service.getService().apply();
			responseHandler.onResponse(new XtextWebOkResponse(request, serviceResult));
		} catch (InvalidRequestException e) {
			responseHandler.onResponse(new XtextWebErrorResponse(request, XtextWebErrorKind.REQUEST_ERROR, e));
		} catch (RuntimeException e) {
			responseHandler.onResponse(new XtextWebErrorResponse(request, XtextWebErrorKind.SERVER_ERROR, e));
		} finally {
			synchronized (callPendingLock) {
				for (var message : pendingPushMessages) {
					try {
						responseHandler.onResponse(message);
					} catch (ResponseHandlerException | RuntimeException e) {
						LOG.error("Error while flushing push message", e);
					}
				}
				pendingPushMessages.clear();
				callPending = false;
			}
		}
	}

	@Override
	public void onPrecomputedServiceResult(String resourceId, String stateId, String serviceName,
			IServiceResult serviceResult) throws ResponseHandlerException {
		var message = new XtextWebPushMessage(resourceId, stateId, serviceName, serviceResult);
		synchronized (callPendingLock) {
			// If we're currently responding to a call we must delay any push messages until
			// the reply is sent, because push messages relating to the new state id must be
			// sent after the response with the new state id so that the client knows about
			// the new state when it receives the push message.
			if (callPending) {
				pendingPushMessages.add(message);
			} else {
				responseHandler.onResponse(message);
			}
		}
	}

	@Override
	public void onSubscribeToPrecomputationEvents(String resourceId, PushWebDocument document) {
		PushWebDocument previousDocument = null;
		var previousSubscription = subscriptions.get(resourceId);
		if (previousSubscription != null) {
			previousDocument = previousSubscription.get();
		}
		if (previousDocument == document) {
			return;
		}
		if (previousDocument != null) {
			previousDocument.removePrecomputationListener(this);
		}
		subscriptions.put(resourceId, new WeakReference<>(document));
	}

	/**
	 * Get the injector to satisfy the request in the {@code serviceContext}.
	 * 
	 * Based on {@link org.eclipse.xtext.web.servlet.XtextServlet#getInjector}.
	 * 
	 * @param serviceContext the Xtext service context of the request
	 * @return the injector for the Xtext language in the request
	 * @throws UnknownLanguageException if the Xtext language cannot be determined
	 */
	protected Injector getInjector(IServiceContext context) {
		IResourceServiceProvider resourceServiceProvider = null;
		var resourceName = context.getParameter("resource");
		if (resourceName == null) {
			resourceName = "";
		}
		var emfURI = URI.createURI(resourceName);
		var contentType = context.getParameter("contentType");
		if (Strings.isNullOrEmpty(contentType)) {
			resourceServiceProvider = resourceServiceProviderRegistry.getResourceServiceProvider(emfURI);
			if (resourceServiceProvider == null) {
				if (emfURI.toString().isEmpty()) {
					throw new UnknownLanguageException(
							"Unable to identify the Xtext language: missing parameter 'resource' or 'contentType'.");
				} else {
					throw new UnknownLanguageException(
							"Unable to identify the Xtext language for resource " + emfURI + ".");
				}
			}
		} else {
			resourceServiceProvider = resourceServiceProviderRegistry.getResourceServiceProvider(emfURI, contentType);
			if (resourceServiceProvider == null) {
				throw new UnknownLanguageException(
						"Unable to identify the Xtext language for contentType " + contentType + ".");
			}
		}
		return resourceServiceProvider.get(Injector.class);
	}

	@Override
	public void dispose() {
		for (var subscription : subscriptions.values()) {
			var document = subscription.get();
			if (document != null) {
				document.removePrecomputationListener(this);
			}
		}
	}
}
