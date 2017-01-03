package cz.cas.mbu.cygenexpi.internal.ui;

import java.awt.Desktop;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

public class LinkSupport {
	public static class LinkMouseListener extends MouseAdapter {
		private final String link;			
		
	    public LinkMouseListener(String link) {
			super();
			this.link = link;
		}

		@Override
	    public void mouseClicked(java.awt.event.MouseEvent evt) {
			openLink(link);
	    }
	}

	public static void openLink(String link)
	{
        try {
            URI uri = new java.net.URI(link);
            (new LinkRunner(uri)).execute();
        } catch (URISyntaxException use) {
            throw new AssertionError(use.getMessage() + ": " + link, use); //NOI18N
        }		
	}
	
	private static class LinkRunner extends SwingWorker<Void, Void> {

	    private final URI uri;

	    private LinkRunner(URI u) {
	        if (u == null) {
	            throw new NullPointerException();
	        }
	        uri = u;
	    }

	    @Override
	    protected Void doInBackground() throws Exception {
		    if (!Desktop.isDesktopSupported()) {
		        return null;
		    }
		    
	        Desktop desktop = java.awt.Desktop.getDesktop();
		    if (desktop.isSupported(Desktop.Action.BROWSE)) {
		        desktop.browse(uri);
		    }
	        return null;
	    }

	    @Override
	    protected void done() {
	        try {
	            get();
	        } catch (ExecutionException ee) {
	            handleException(uri, ee);
	        } catch (InterruptedException ie) {
	            handleException(uri, ie);
	        }
	    }

	    private static void handleException(URI u, Exception e) {
	        JOptionPane.showMessageDialog(null, "Sorry, a problem occurred while trying to open this link in your system's standard browser.", "A problem occured", JOptionPane.ERROR_MESSAGE);
	    }
	}
}
