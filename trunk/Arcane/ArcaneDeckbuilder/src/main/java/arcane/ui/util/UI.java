/**
 *     Copyright (C) 2010  snacko
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


package arcane.ui.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.ViewportLayout;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.ImageView;

/**
 * UI utility functions.
 *
 * @author dhudson
 * @version $Id: $
 */
public class UI {
    static private Hashtable<URL, Image> imageCache = new Hashtable();

    /**
     * <p>getToggleButton</p>
     *
     * @return a {@link javax.swing.JToggleButton} object.
     */
    static public JToggleButton getToggleButton() {
        JToggleButton button = new JToggleButton();
        button.setMargin(new Insets(2, 4, 2, 4));
        return button;
    }

    /**
     * <p>getButton</p>
     *
     * @return a {@link javax.swing.JButton} object.
     */
    static public JButton getButton() {
        JButton button = new JButton();
        button.setMargin(new Insets(2, 4, 2, 4));
        return button;
    }

    /**
     * <p>setTitle</p>
     *
     * @param panel a {@link javax.swing.JPanel} object.
     * @param title a {@link java.lang.String} object.
     */
    static public void setTitle(JPanel panel, String title) {
        Border border = panel.getBorder();
        if (border instanceof TitledBorder) {
            ((TitledBorder) panel.getBorder()).setTitle(title);
            panel.repaint();
        } else
            panel.setBorder(BorderFactory.createTitledBorder(title));
    }

    /**
     * <p>getFileURL</p>
     *
     * @param path a {@link java.lang.String} object.
     * @return a {@link java.net.URL} object.
     */
    static public URL getFileURL(String path) {
        File file = new File(path);
        if (file.exists()) {
            try {
                return file.toURL();
            } catch (MalformedURLException ignored) {
            }
        }
        return UI.class.getResource(path);
    }

    /**
     * <p>getImageIcon</p>
     *
     * @param path a {@link java.lang.String} object.
     * @return a {@link javax.swing.ImageIcon} object.
     */
    static public ImageIcon getImageIcon(String path) {
        try {
            InputStream stream;
            stream = UI.class.getResourceAsStream(path);
            if (stream == null && new File(path).exists()) stream = new FileInputStream(path);
            if (stream == null) throw new RuntimeException("Image not found: " + path);
            byte[] data = new byte[stream.available()];
            stream.read(data);
            return new ImageIcon(data);
        } catch (IOException ex) {
            throw new RuntimeException("Error reading image: " + path);
        }
    }

    /**
     * <p>setHTMLEditorKit</p>
     *
     * @param editorPane a {@link javax.swing.JEditorPane} object.
     */
    static public void setHTMLEditorKit(JEditorPane editorPane) {
        editorPane.getDocument().putProperty("imageCache", imageCache); // Read internally by ImageView, but never written.
        // Extend all this shit to cache images.
        editorPane.setEditorKit(new HTMLEditorKit() {
            public ViewFactory getViewFactory() {
                return new HTMLFactory() {
                    public View create(Element elem) {
                        Object o = elem.getAttributes().getAttribute(StyleConstants.NameAttribute);
                        if (o instanceof HTML.Tag) {
                            HTML.Tag kind = (HTML.Tag) o;
                            if (kind == HTML.Tag.IMG) return new ImageView(elem) {
                                public URL getImageURL() {
                                    URL url = super.getImageURL();
                                    // Put an image into the cache to be read by other ImageView methods.
                                    if (url != null && imageCache.get(url) == null)
                                        imageCache.put(url, Toolkit.getDefaultToolkit().createImage(url));
                                    return url;
                                }
                            };
                        }
                        return super.create(elem);
                    }
                };
            }
        });
    }

    /**
     * <p>setVerticalScrollingView</p>
     *
     * @param scrollPane a {@link javax.swing.JScrollPane} object.
     * @param view a {@link java.awt.Component} object.
     */
    static public void setVerticalScrollingView(JScrollPane scrollPane, final Component view) {
        final JViewport viewport = new JViewport();
        viewport.setLayout(new ViewportLayout() {
            public void layoutContainer(Container parent) {
                viewport.setViewPosition(new Point(0, 0));
                Dimension viewportSize = viewport.getSize();
                int width = viewportSize.width;
                int height = Math.max(view.getPreferredSize().height, viewportSize.height);
                viewport.setViewSize(new Dimension(width, height));
            }
        });
        viewport.setView(view);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setViewport(viewport);
    }

    /**
     * <p>getDisplayManaCost</p>
     *
     * @param manaCost a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    static public String getDisplayManaCost(String manaCost) {
        manaCost = manaCost.replace("/", "{slash}");
        // A pipe in the cost means "process left of the pipe as the card color, but display right of the pipe as the cost".
        int pipePosition = manaCost.indexOf("{|}");
        if (pipePosition != -1) manaCost = manaCost.substring(pipePosition + 3);
        return manaCost;
    }

    /**
     * <p>invokeLater</p>
     *
     * @param runnable a {@link java.lang.Runnable} object.
     */
    static public void invokeLater(Runnable runnable) {
        EventQueue.invokeLater(runnable);
    }

    /**
     * <p>invokeAndWait</p>
     *
     * @param runnable a {@link java.lang.Runnable} object.
     */
    static public void invokeAndWait(Runnable runnable) {
        if (EventQueue.isDispatchThread()) {
            runnable.run();
            return;
        }
        try {
            EventQueue.invokeAndWait(runnable);
        } catch (InterruptedException ex) {
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * <p>setSystemLookAndFeel</p>
     */
    static public void setSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            System.err.println("Error setting look and feel:");
            ex.printStackTrace();
        }
    }

    /**
     * <p>setDefaultFont</p>
     *
     * @param font a {@link java.awt.Font} object.
     */
    static public void setDefaultFont(Font font) {
        for (Object key : Collections.list(UIManager.getDefaults().keys())) {
            Object value = UIManager.get(key);
            if (value instanceof javax.swing.plaf.FontUIResource) UIManager.put(key, font);
        }
    }
}