# Civilizations player and server guide

The living handbook is [PLAYER_AND_SERVER_GUIDE.md](PLAYER_AND_SERVER_GUIDE.md).
The readable PDF is [Civilizations Player and Server Guide](../../output/pdf/Civilizations_Player_and_Server_Guide.pdf).

Edition 0.8 contains a quick overview, TL DR, chapter list, 24 feature/reference
chapters, player/admin commands, permissions, troubleshooting, server procedures,
terms and a connected village example. Each feature identifies implemented,
development-only or planned behavior. Off-limits plot selection/protection and confirmed release and read-only saved-housing inspection with configurable covered beds now work. Single-home donation, live whole-family housing and confirmed donor repossession are available. Editable dwelling units and apartment controls remain unfinished.

## Update workflow

1. Update the Markdown alongside meaningful feature batches. Treat current code
   and PROJECT_STATUS.md as implementation authority; dated feature notes can lag.
2. Verify commands and permission defaults against the handlers and plugin.yml.
   Update the edition, reviewed application baseline and implementation labels.
3. Run `python tools/build_player_guide.py` in an environment with reportlab.
   Codex uses its bundled dependency runtime, without installing repo dependencies.
4. Render the PDF with Poppler and inspect every page, including wrapped command
   syntax, tables, page numbers, bookmarks and chapter links. Keep QA images in a
   temporary folder rather than publishing them as guide assets.
   PDFs are explicitly marked binary in .gitattributes so Windows checkout does
   not rewrite line endings inside their byte-offset-based structure.
5. Commit the editable source, builder and final PDF together. Add screenshots
   after actual controls exist. No Maven/server/database operation is required for
   a prose-only handbook update.

New chapters should explain what the feature does, how to use it, what affects or
pauses it, and its relationships to other systems. Keep examples practical and
distinguish supported behavior from future intent. No new player command or
gameplay rule becomes implemented merely by appearing in the guide.
