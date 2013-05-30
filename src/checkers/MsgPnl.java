package checkers;
import treegame.*;
import java.awt.*;
import mytools.*;

public class MsgPnl extends FancyPanel {

//	private ThrCmd inCmd;		// command is not used, except to synchronize access.
//	private String msg;
//	private TextArea lbl;

	MsgPnl() {
		super(null, STYLE_PLAIN, new Color(200,200,180));

		setLayout(new BorderLayout());

		lbl = new TextArea("",2,40,TextArea.SCROLLBARS_NONE);
		lbl.setEditable(false);

		lbl.setFont(new Font("TimesRoman", Font.BOLD, 14));
		add(lbl, "Center");
	}

   public void clear() {
      set("");
   }

	public void set(String msg) {
		lbl.setText(msg);
	}

	private TextArea lbl;

}
