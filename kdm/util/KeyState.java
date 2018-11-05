package kdm.util;

import java.awt.*;
import java.awt.event.*;

/** keeps track of key state */
public class KeyState implements AWTEventListener
{
   protected static KeyState keyState = null;
   public static int nKeys = 1024;
   protected boolean bDown[];
   
   protected KeyState()
   {
      assert(keyState == null);
      Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.KEY_EVENT_MASK);
      keyState = this;
      bDown = new boolean[nKeys];
   }
   
   /** setup the key listener (will be done automatically if necessary upon first query) */
   public static void setup()
   {
      if (keyState == null) keyState = new KeyState();
   }
   
   /** @return current state (true=down) for the given key code */
   public static boolean getKeyState(int vk)
   {
      if (vk >= nKeys){
         System.err.printf("Warning: KeyState only supports virtual key codes < %d.\n", nKeys);
         System.err.printf(" Fix: set KeyState.nKeys before querying key state or calling setup.");
         return false;
      }
      if (keyState == null) keyState = new KeyState();
      return keyState.bDown[vk];      
   }
   
   public void eventDispatched(AWTEvent event)
   {
      KeyEvent key = (KeyEvent)event;
      if (key.isConsumed()) return;
      // TODO peek at event queue and make sure there isn't a simultaneous press/release?
      int id = event.getID();
      int vk = key.getKeyCode();
      if (id == KeyEvent.KEY_PRESSED) bDown[vk] = true;
      else if (id == KeyEvent.KEY_RELEASED) bDown[vk] = false;
   }

}
