package com.notesplus;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

class NotesAutosaveController
{
	private static final long SAVE_DEBOUNCE_MILLIS = 500L;

	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	private final Runnable saveAction;
	private ScheduledFuture<?> pendingSave;

	NotesAutosaveController(Runnable saveAction)
	{
		this.saveAction = saveAction;
	}

	synchronized void requestSave()
	{
		if (pendingSave != null)
		{
			pendingSave.cancel(false);
		}

		pendingSave = scheduler.schedule(saveAction, SAVE_DEBOUNCE_MILLIS, TimeUnit.MILLISECONDS);
	}

	synchronized void flushAndShutdown()
	{
		if (pendingSave != null)
		{
			pendingSave.cancel(false);
			pendingSave = null;
		}
		saveAction.run();
		scheduler.shutdown();
	}
}
