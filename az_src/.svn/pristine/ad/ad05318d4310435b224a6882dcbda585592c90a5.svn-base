package org.gudy.azureus2.ui.swt.progress;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.util.CopyOnWriteList;

/**
 * A implementation of <code>IProgressReporter</code>
 * <p>Any process wishing to participate in providing global progress indication can instantiate
 * this class and simply use the available methods so set values or issue a command
 * 
 * <p>
 * When ever any state changes in this reporter a notification will be sent to the global
 * reporting manager {@link ProgressReportingManager} followed by a notification to all
 * registered listeners of this reporter</p>
 * 
 * The listeners will be passed an immutable progress report {@link ProgressReporter.ProgressReport} which
 * represents a snapshot of all the public properties contained in this reporter; inspecting the
 * ProgressReport is the only way a listener can query the properties of this reporter.  This pattern
 * allows the ProgressReporter to continue and process requests by not having to thread lock all public
 * methods.  Additionally, the listeners are guaranteed a consistent snapshot of the reporter.
 * 
 * <p>
 * This reporter also has the capability to receive loop back commands from a listener for actions such like
 * {@link #cancel()} and {@link #retry()}.  These commands are enabled by calling 
 * 		{@link ProgressReporter#setCancelAllowed(boolean)} 
 * or {@link ProgressReporter#setRetryAllowed(boolean)}.
 * 
 * The listener only initiates these actions by sending a notification back to the owner of the reporter;
 * it is up the to owner to perform the actual act of canceling or retrying.
 * </p><p>
 * 
 * A typical life cycle of the ProgresReporter as seen from an owner is as follows 
 * (an owner is defined as the process that created the reporter):
 * <ul>
 * <li>Create ProgressReporter</li>
 * <li>Set initial properties</li>
 * <li>Register a listener to the reporter to respond to loopback notifications (optional)</li>
 * <li>Update the reporter</li>
 * <ul>
 * <li>Set selection or percentage [{@link ProgressReporter#setSelection(int, String)}, {@link ProgressReporter#setPercentage(int, String)}]</li>
 * <li>Set message [{@link ProgressReporter#setMessage(String)}]</li>
 * <li>...</li>
 * <li>Repeat until done</li>
 * <li>Set done [{@link ProgressReporter#setDone()}]</li>
 * </ul>
 * 
 * <li>Then optionally Dispose of the reporter [{@link ProgressReporter#dispose(Object)}]</li>.<p>
 * In addition to internal clean-ups, calling dispose(Object) will effectively remove the reporter from the history stack of the
 * reporting manager and no more messages from this reporter will be processed.</P>
 * </ul></p><p>
 * 
 * Once a reporter is created and any property in the reporter is set the global reporting manager is
 * notified; at which point any listener listening to the manager is forwarded this reporter.
 * The manager listener may decide to display this reporter in a UI element, may register specific
 * listeners to this reporter, may query its properties and take action, or can simply monitor it
 * for such functions as logging.</p>
 * 
 * This implementation is non-intrusive to the owner process and so provides existing processes the
 * ability to participate in global progress indication without significant modification to the underlying
 * processes.
 * 
 * 
 * 
 * @author knguyen
 *
 */
public class ProgressReporter
	implements IProgressReporter, IProgressReportConstants
{

	private ProgressReportingManager manager = null;

	/**
	 * An instance id for this reporter that is guaranteed to be unique within this same session 
	 * It is declared as transient to help ensure no accidental reliance on the persistence of this ID
	 * between sessionS (just in case this class is declared serializable in a future modification)
	 */
	private transient int ID;

	private int minimum, maximum, selection, percentage;

	private int latestReportType = REPORT_TYPE_INIT;

	private boolean isIndeterminate, isDone, isPercentageInUse, isCancelAllowed,
			isCanceled, isRetryAllowed, isInErrorState, isDisposed;

	private String title = "";

	private String message = "";

	/**
	 * Accumulates the detail messages in a List
	 * <p>This is for when a listener starts listening to this reporter after it has started running;
	 * upon initialization the listener may query this list to get all messages sent up to that point.</p>
	 * 
	 */
	private CopyOnWriteList messageHistory = new CopyOnWriteList();

	private String detailMessage = "";

	private String errorMessage = "";

	private String name = "";

	private Image image = null;

	private String reporterType = REPORTER_TYPE_DEFAULT;

	private IProgressReport latestProgressReport = null;

	private CopyOnWriteList reporterListeners = null; //KN: Lazy init since not all reporters will have direct listeners

	/**
	 * An arbitrary object reference that can be used by the owner of the <code>ProgressReporter</code> and its
	 * listeners to share additional information should the current implementation be insufficient
	 */
	private Object objectData = null;

	private int messageHistoryLimit = 1000;

	/**
	 * If any of the following states have been reached then the reporter is considered inactive:
	 * <ul>
	 * <li><code>isDisposed</code> 			== 	<code>true</code></li>
	 * <li><code>isDone</code> 					== 	<code>true</code></li>
	 * <li><code>isInErrorState</code>	==	<code>true</code></li>
	 * <li><code>isCanceled</code> 			==	<code>true</code></li>
	 * </ul>
	 */
	private boolean isActive = true;

	/**
	 * Construct a <code>ProgressReporter</code>; the returned instance is initialized with the proper ID
	 */
	protected ProgressReporter(ProgressReportingManager manager) {
		this(manager,null);
	}

	/**
	 * Construct a <code>ProgressReporter</code> with the given <code>name</code>; the returned
	 * instance would have been initialized with the proper ID
	 * @param name
	 */
	protected ProgressReporter(ProgressReportingManager _manager, String name) {
		manager = _manager;
		this.name = name;
		this.ID = manager.getNextAvailableID();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#setReporterType(String)
	 */
	public void setReporterType(String reporterType) {
		this.reporterType = reporterType;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#dispose()
	 */
	public void dispose() {

		synchronized (this) {
			/*
			 * Disposed already so no need to do it again
			 */

			if (true == isDisposed) {
				return;
			}

			isDisposed = true;
			isActive = false;
		}

		latestReportType = REPORT_TYPE_DISPOSED;

		/*
		 * Notifies listeners
		 */
		notifyListeners( getProgressReport());

		/*
		 * Then dispose all listeners to let GC do it's magic.
		 * This is just to do final clearing since at this point most listeners should have been disposed
		 * already.
		 */
		if (null != reporterListeners) {
			reporterListeners.clear();
		}

		/*
		 * Finally notifies the manager
		 */
		manager.notifyManager(this);
	}

	/**
	 * Resets this reporter to its initial states such that values are reset to default
	 * <p>An appropriate use for this is when a process is restarting or retrying; this allows an owning process
	 * to keep on using the same instance of this reporter without having to create and dispatch a new one</p>
	 */
	private void reInit() {
		isActive = true;
		isCanceled = false;
		isDone = false;
		isInErrorState = false;
		errorMessage = "";
		message = "";
		detailMessage = "";
		messageHistory.clear();
	}

	/**
	 * Notifies registered listener that an event has occurred.
	 * Subsequently a listener may be removed if it returns the value of  <code>RETVAL_OK_TO_DISPOSE</code>;
	 * this optimization is designed to prevent dangling/orphaned listeners, and also reduces the
	 * number of listeners to notify upon the next event 
	 */
	private void notifyListeners( IProgressReport report) {
		if (null == reporterListeners || reporterListeners.size() < 1) {
			return;
		}

		List removalList = new ArrayList();

		for (Iterator iterator = reporterListeners.iterator(); iterator.hasNext();) {
			IProgressReporterListener listener = ((IProgressReporterListener) iterator.next());

			try {
				/*
				 * If the listener returned RETVAL_OK_TO_DISPOSE then it has indicated that it is no longer needed so we release it
				 */
				if (RETVAL_OK_TO_DISPOSE == listener.report( report )) {
					removalList.add(listener);
				}
			} catch (Throwable e) {
				Debug.out(e);
			}
		}

		/*
		 * Removes any listeners marked ok to disposed
		 */
		for (Iterator iterator = removalList.iterator(); iterator.hasNext();) {
			reporterListeners.remove(iterator.next());
		}
	}

	/**
	 * Updates and notifies listeners
	 * @param REPORT_TYPE
	 */
	private void updateAndNotify(int eventType) {
		latestReportType = eventType;

		/*
		 * Take a snap shot of the reporter
		 */
		latestProgressReport = new ProgressReport();

		/*
		 * We directly bubble up this event to the manager for efficiency;
		 * as opposed to having the manager register as a listener to each and every ProgressReporter.
		 * Effectively this allows the manager to receive all events from all reporters 
		 */
		manager.notifyManager(this);

		/*
		 * If a property has changed but the reporter has been canceled then don't notify the listener
		 * since they are no longer expecting a REPORT_TYPE_PROPERTY_CHANGED event
		 */
		if (eventType == REPORT_TYPE_PROPERTY_CHANGED && true == isCanceled) {
			return;
		}

		/*
		 * Now we can notify listeners for this specific reporter
		 */
		notifyListeners( latestProgressReport );
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#setSelection(int, java.lang.String)
	 */
	public void setSelection(int selection, String message) {
		if (true == shouldIgnore()) {
			return;
		}
		this.message = message;

		if (selection >= maximum) {
			setDone();
			return;
		}
		if (selection < minimum) {
			percentage = 0;
			selection = minimum;
			isIndeterminate = true;
			return;
		}
		this.selection = selection;
		percentage = (selection * 100) / (maximum - minimum);
		isDone = false;
		isPercentageInUse = false;
		isIndeterminate = false;

		if (null != message && message.length() > 0) {
			addToMessageHistory(message, MSG_TYPE_INFO);
		}

		updateAndNotify(REPORT_TYPE_PROPERTY_CHANGED);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#setPercentage(int, java.lang.String)
	 */
	public void setPercentage(int percentage, String message) {
		if (true == shouldIgnore()) {
			return;
		}

		this.message = message;

		if (percentage >= 100) {
			setDone();
			return;
		}

		if (percentage < 0) {
			percentage = 0;
			selection = minimum;
			isIndeterminate = true;
			return;
		}
		minimum = 0;
		maximum = 100;
		this.percentage = percentage;
		this.selection = percentage;
		isDone = false;
		isPercentageInUse = true;
		isIndeterminate = false;

		if (null != message && message.length() > 0) {
			addToMessageHistory(message, MSG_TYPE_INFO);
		}

		updateAndNotify(REPORT_TYPE_PROPERTY_CHANGED);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#setIndeterminate(boolean)
	 */
	public void setIndeterminate(boolean isIndeterminate) {
		if (true == shouldIgnore()) {
			return;
		}

		this.isIndeterminate = isIndeterminate;
		if (true == isIndeterminate) {
			minimum = 0;
			maximum = 0;
		}
		updateAndNotify(REPORT_TYPE_MODE_CHANGE);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#setDone()
	 */
	public void setDone() {
		synchronized (this) {
			if (true == shouldIgnore()) {
				return;
			}

			isDone = true;
			isActive = false;
		}

		selection = maximum;
		percentage = 100;
		isIndeterminate = false;
		message = MessageText.getString("Progress.reporting.status.finished");
		addToMessageHistory(message, MSG_TYPE_LOG);
		updateAndNotify(REPORT_TYPE_DONE);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#setMessage(java.lang.String)
	 */
	public void setMessage(String message) {
		if (true == shouldIgnore()) {
			return;
		}
		this.message = message;
		addToMessageHistory(message, MSG_TYPE_INFO);
		updateAndNotify(REPORT_TYPE_PROPERTY_CHANGED);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#setDetailMessage(java.lang.String)
	 */
	public void appendDetailMessage(String detailMessage) {
		if (true == shouldIgnore()) {
			return;
		}
		this.detailMessage = detailMessage;

		addToMessageHistory(detailMessage, MSG_TYPE_LOG);

		updateAndNotify(REPORT_TYPE_PROPERTY_CHANGED);

		/*
		 * The detail message operates in append mode so after we have notified all the listeners
		 * we reset it.  It is up to the listeners to accumulate the messages.
		 * 
		 * Lazy implementor can also simply query the messageHistory
		 */
		this.detailMessage = "";
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#setMinimum(int)
	 */
	public void setMinimum(int min) {
		if (true == shouldIgnore()) {
			return;
		}
		this.minimum = min;
		updateAndNotify(REPORT_TYPE_PROPERTY_CHANGED);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#setMaximum(int)
	 */
	public void setMaximum(int max) {
		if (true == shouldIgnore()) {
			return;
		}
		this.maximum = max;
		updateAndNotify(REPORT_TYPE_PROPERTY_CHANGED);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#cancel()
	 */
	public void cancel() {
		synchronized (this) {
			if (true == isCanceled || true == shouldIgnore()) {
				return;
			}

			isCanceled = true;
			isActive = false;
		}
		message = MessageText.getString("Progress.reporting.status.canceled");
		addToMessageHistory(message, MSG_TYPE_LOG);
		updateAndNotify(REPORT_TYPE_CANCEL);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#retry()
	 */
	public void retry() {
		synchronized (this) {
			if (true == shouldIgnore()) {
				return;
			}
			reInit();
		}
		message = MessageText.getString("Progress.reporting.status.retrying");
		addToMessageHistory(message, MSG_TYPE_LOG);
		updateAndNotify(REPORT_TYPE_RETRY);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#setCancelAllowed(boolean)
	 */
	public void setCancelAllowed(boolean cancelAllowed) {
		if (true == shouldIgnore()) {
			return;
		}

		this.isCancelAllowed = cancelAllowed;
		updateAndNotify(REPORT_TYPE_PROPERTY_CHANGED);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#setName(java.lang.String)
	 */
	public void setName(String name) {
		if (true == shouldIgnore()) {
			return;
		}
		this.name = name + ""; //KN: Just a quick way to ensure the name is not null
		updateAndNotify(REPORT_TYPE_PROPERTY_CHANGED);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#setTitle(java.lang.String)
	 */
	public void setTitle(String title) {
		if (true == shouldIgnore()) {
			return;
		}
		this.title = title;
		updateAndNotify(REPORT_TYPE_PROPERTY_CHANGED);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#setImage(org.eclipse.swt.graphics.Image)
	 */
	public void setImage(Image image) {
		if (true == shouldIgnore()) {
			return;
		}
		this.image = image;
		updateAndNotify(REPORT_TYPE_PROPERTY_CHANGED);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#setErrorMessage(java.lang.String)
	 */
	public void setErrorMessage(String errorMessage) {
		if (true == shouldIgnore()) {
			return;
		}

		/*
		 * Ignores duplicate error messages
		 * NOTE: TorrentDownloaderImpl#runSupport() has a loop that currently sends the same error message
		 * multiple times; there may be other processes doing the same thing so this code is meant to prevent
		 * forwarding any error message from the same reporter more than once.
		 */
		if (null != this.errorMessage
				&& true == this.errorMessage.equals(errorMessage)) {
			return;
		}

		if (null == errorMessage || errorMessage.length() < 1) {
			this.errorMessage = MessageText.getString("Progress.reporting.default.error");
		} else {
			this.errorMessage = errorMessage;
		}
		isInErrorState = true;
		isActive = false;
		addToMessageHistory(this.errorMessage, MSG_TYPE_ERROR);
		updateAndNotify(REPORT_TYPE_ERROR);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#setRetryAllowed(boolean)
	 */
	public void setRetryAllowed(boolean retryOnError) {
		if (true == shouldIgnore()) {
			return;
		}
		this.isRetryAllowed = retryOnError;
	}

	/**
	 * A convenience method to return whether this reporter should ignore subsequent calls to its accessor methods.
	 * When a reporter has reached this state listeners usually make the assumption that there will be no
	 * more changes to any properties in this reporter.  The use of this method is intended to provide a more consistent
	 * state for the reporter; without this check a listener may have reference to a reporter that is marked as <code>done</code>
	 * but then a subsequent message says it's at 50% completion.  By cutting of the notification here we prevent such 
	 * ambiguity from occurring for the listeners.
	 * <p>
	 * If any of the following states have been reached then the reporter can ignore subsequent calls to its accessor methods:
	 * <ul>
	 * <li><code>isDisposed</code> 			== 	<code>true</code></li>
	 * <li><code>isDone</code> 					== 	<code>true</code></li>
	 * </ul>
	 * </p>
	 * @return
	 */
	private boolean shouldIgnore() {
		return (true == isDisposed || true == isDone);
	}

	/**
	 * Create and add an <code>IMessage</code> to the message history
	 * @param value
	 * @param type
	 */
	private void addToMessageHistory(String value, int type) {

		/*
		 * Limiting the history list to prevent runaway processes from taking up too much resources
		 */
		if (messageHistory.size() >= messageHistoryLimit) {
			return;
		}

		if (messageHistory.size() < messageHistoryLimit) {
			messageHistory.add(new ProgressReportMessage(value, type));
		}

		if (messageHistory.size() == messageHistoryLimit) {
			Debug.out(MessageText.getString(
					"Progress.reporting.detail.history.limit", new String[] {
						messageHistoryLimit + ""
					}));
		}
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#setObjectData(java.lang.Object)
	 */
	public void setObjectData(Object objectData) {
		if (true == shouldIgnore()) {
			return;
		}
		this.objectData = objectData;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#getMessageHistory()
	 */
	public IMessage[] getMessageHistory() {
		/*
		 * Converting to array so the original list is insulated from modification
		 */
		List tmp = messageHistory.getList();
		return (IMessage[]) tmp.toArray(new IMessage[tmp.size()]);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#addListener(org.gudy.azureus2.ui.swt.mainwindow.IProgressReporterListener)
	 */
	public void addListener(IProgressReporterListener listener) {
		if (true == shouldIgnore()) {
			return;
		}
		if (null != listener) {
			if (null == reporterListeners) {
				reporterListeners = new CopyOnWriteList();
				reporterListeners.add(listener);
			} else if (false == reporterListeners.contains(listener)) {
				reporterListeners.add(listener);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#removeListener(org.gudy.azureus2.ui.swt.mainwindow.IProgressReporterListener)
	 */
	public void removeListener(IProgressReporterListener listener) {
		if (null == reporterListeners) {
			return;
		}
		reporterListeners.remove(listener);
	}

	/**
	 * Numerically compare by reporter ID's
	 */
	public int compareTo(Object obj) {
		if (obj instanceof IProgressReporter) {
			//KN: Will this always work as expected as opposed to using ((IProgressReporter)obj).getID()?
			return (ID < obj.hashCode() ? -1 : (ID == obj.hashCode() ? 0 : 1));
		}
		return 0;
	}

	/**
	 * Reporters are equal iif they have the same ID
	 */
	public boolean equals(Object obj) {
		if (obj instanceof IProgressReporter) {
			//KN: Will this always work as expected as opposed to using ((IProgressReporter)obj).getID()?
			return ID == obj.hashCode();
		}
		return false;
	}

	/**
	 * Hashcode and ID are the same
	 */
	public int hashCode() {
		return ID;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#getProgressReport()
	 */
	public IProgressReport getProgressReport() {
		if (null == latestProgressReport) {
			latestProgressReport = new ProgressReport();
		}
		return latestProgressReport;
	}

	/**
	 * An immutable object containing all interesting values in a <code>ProgressReporter</code>.
	 * <p>This represents a snapshot of all values at a single moment so instantiation of this class
	 * should be guarded against multi-threaded modification of the source <code>ProgressReporter</code> </p>
	 * 
	 * <p>This class is the only way an observer can query the properties of a <code>ProgressReporter</code>;
	 * though they do not have to be, all variables are declared <code>final</code> to help remind the user of this class
	 * that modification to any of its properties would have no effect on the reporter itself.
	 * <p>
	 * An exception to this insulation is the <code>objectData</code> variable; both the reporter
	 * and the ProgressReport consumer have full access to it.  This is to facilitate advanced
	 * 2-way communication between the 2 parties.</p> 
	 * 
	 * 
	 * @author knguyen
	 * @see ProgressReporter#getProgressReport()
	 */
	public class ProgressReport
		implements IProgressReport
	{
		private final String reporterType = ProgressReporter.this.reporterType;

		private final int reporterID = ProgressReporter.this.ID;

		private final int minimum = ProgressReporter.this.minimum;

		private final int maximum = ProgressReporter.this.maximum;

		private final int selection = ProgressReporter.this.selection;

		private final int percentage = ProgressReporter.this.percentage;

		private final boolean isActive = ProgressReporter.this.isActive;

		private final boolean isIndeterminate = ProgressReporter.this.isIndeterminate;

		private final boolean isDone = ProgressReporter.this.isDone;

		private final boolean isPercentageInUse = ProgressReporter.this.isPercentageInUse;

		private final boolean isCancelAllowed = ProgressReporter.this.isCancelAllowed;

		public final boolean isCanceled = ProgressReporter.this.isCanceled;

		private final boolean isRetryAllowed = ProgressReporter.this.isRetryAllowed;

		private final boolean isInErrorState = ProgressReporter.this.isInErrorState;

		private final boolean isDisposed = ProgressReporter.this.isDisposed;

		private final String title = ProgressReporter.this.title;

		private final String message = ProgressReporter.this.message;

		private final String detailMessage = ProgressReporter.this.detailMessage;

		private final String errorMessage = ProgressReporter.this.errorMessage;

		private final String name = ProgressReporter.this.name;

		private final Image image = ProgressReporter.this.image;

		private final Object objectData = ProgressReporter.this.objectData;

		private final int REPORT_TYPE = ProgressReporter.this.latestReportType;

		/**
		 * Construct a ProgressReport
		 */
		private ProgressReport() {
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#getReporterType()
		 */
		public String getReporterType() {
			return reporterType;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#getReporterID()
		 */
		public int getReporterID() {
			return reporterID;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#getMinimum()
		 */
		public int getMinimum() {
			return minimum;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#getMaximum()
		 */
		public int getMaximum() {
			return maximum;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#getSelection()
		 */
		public int getSelection() {
			return selection;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#getPercentage()
		 */
		public int getPercentage() {
			return percentage;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#isActive()
		 */
		public boolean isActive() {
			return isActive;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#isIndeterminate()
		 */
		public boolean isIndeterminate() {
			return isIndeterminate;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#isDone()
		 */
		public boolean isDone() {
			return isDone;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#isPercentageInUse()
		 */
		public boolean isPercentageInUse() {
			return isPercentageInUse;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#isCancelAllowed()
		 */
		public boolean isCancelAllowed() {
			return isCancelAllowed;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#isCanceled()
		 */
		public boolean isCanceled() {
			return isCanceled;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#isRetryAllowed()
		 */
		public boolean isRetryAllowed() {
			return isRetryAllowed;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#isInErrorState()
		 */
		public boolean isInErrorState() {
			return isInErrorState;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#isDisposed()
		 */
		public boolean isDisposed() {
			return isDisposed;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#getTitle()
		 */
		public String getTitle() {
			return title;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#getMessage()
		 */
		public String getMessage() {
			return message;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#getDetailMessage()
		 */
		public String getDetailMessage() {
			return detailMessage;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#getErrorMessage()
		 */
		public String getErrorMessage() {
			return errorMessage;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#getName()
		 */
		public String getName() {
			return name;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#getImage()
		 */
		public Image getImage() {
			return image;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#getObjectData()
		 */
		public Object getObjectData() {
			return objectData;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#getREPORT_TYPE()
		 */
		public int getReportType() {
			return REPORT_TYPE;
		}

	}

}