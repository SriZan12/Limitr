# Original UI restoration

Reference: published branch commit `fe69574`. Keep the existing Kotlin/XML design, assets, colours and Acme typography. The modernization changes persistence and enforcement; it does not require a visual redesign.

| Screen | Restored presentation and spacing | Compatibility adjustments |
|---|---|---|
| Home | Centered title, Crypto/coin, overflow menu, existing tabs; no profile icon or horizontal action row. | Show access/storage status only when attention is needed. |
| App lists | Original 8 dp list margins plus 8 dp padding; 40 dp app icons, 20 dp row padding and 10 dp icon/text gap. Restore original blocked-row text sizes and empty-state typography. | Content-height rows prevent large text clipping. Unavailable usage uses a short dash instead of a repeated paragraph; optional permission disclosure stays in setup. |
| Blocking configuration | Original history/bell/switch row, 80 dp app icon, 18 sp Acme name, 3 dp divider, rounded white timer/hourglass cards and Unblock App button. Cards retain 140 dp maximum width, 180 dp minimum height, 12 dp internal padding, 60 dp illustrations and 20 dp image spacing. Unblock retains 30 dp margins. | Cards can narrow and grow vertically; the screen scrolls in landscape/large fonts. Timer and interval values come from the persisted session evaluator. Unavailable enforcement remains visible. |
| Timer picker | Original three wheels, labels, close control and dark Start Timer button. | Equal-width columns and content-height button; scrollable when space is constrained. Keyboard edits are still committed before saving. |
| Unblock popup | Original dark heading, coin, 2-Crypto cost and Unblock App button. | Keep a secondary Stop session action and confirmation so intentional cancellation does not depend on credits. Errors stay visible; pending operations disable actions. |
| Blocking overlay | Original white 90 dp icon card, uppercase blocked-app message, Remaining Time/countdown and bottom details/exit buttons with 24 dp margins. | Scroll when required; retain the service-owned accessibility window, insets, one countdown and expiry removal. Details/Home remain explicit user actions. |
| History | Original dark screen, toolbar and icon/text rows with 10 dp list margins, 8 dp padding and 20 dp row padding. | Render real counts/dates from the existing lifecycle-aware Room flow; show migration/storage messages only when applicable. |
| Permissions and rewards | Original styled popup sequence restored in the preceding change. | Affirmative consent, optional usage access and atomic daily claims remain. |
| Video | Original full-screen player presentation and controls retained. | Player release and insets remain; the old orphaned loading spinner is not restored. |
| Help | Original instruction sections and Watch a Video / Grant Permission buttons. | Scrollable content; device/version guidance and consent are retained. |

System-bar/cutout/keyboard insets are additive to design spacing. Scroll support and minimum 48 dp controls prevent the original fixed dimensions from hiding actions. Firebase sign-in and the home profile entry remain removed as requested; dormant legacy layouts do not define the active blocking screen.

Executed checks and remaining device coverage are recorded in [TESTING.md](TESTING.md). No claim is made that all manufacturers, screen sizes or font settings have been tested.
