"""Build the living player handbook PDF from its version-controlled Markdown source.

Run with the Codex bundled Python (reportlab), or another environment providing
reportlab. No server, database, network, or Maven build is accessed.
"""
from pathlib import Path
from html import escape
import argparse
import re
from reportlab.lib import colors
from reportlab.lib.pagesizes import letter
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.platypus import (
    BaseDocTemplate, Frame, PageTemplate, Paragraph, Spacer, PageBreak, Table,
    TableStyle,
)
from reportlab.platypus.tableofcontents import TableOfContents

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'docs/guide/PLAYER_AND_SERVER_GUIDE.md'
OUTPUT = ROOT / 'output/pdf/Civilizations_Player_and_Server_Guide.pdf'


def inline(text):
    text = escape(text)
    text = re.sub(r'`([^`]+)`', r'<font name="Courier">\1</font>', text)
    text = re.sub(r'\*\*([^*]+)\*\*', r'<b>\1</b>', text)
    return text


class Handbook(BaseDocTemplate):
    def afterFlowable(self, flowable):
        if isinstance(flowable, Paragraph) and flowable.style.name == 'Chapter':
            title = flowable.getPlainText()
            key = 'chapter-' + title.split()[0]
            self.canv.bookmarkPage(key)
            self.canv.addOutlineEntry(title, key, 0, False)
            self.notify('TOCEntry', (0, title, self.page, key))


def build(source=SOURCE, output=OUTPUT):
    output.parent.mkdir(parents=True, exist_ok=True)
    text = source.read_text(encoding='utf-8')
    if any(c in text for c in '\u2010\u2011\u2012\u2013\u2014'):
        raise ValueError('Use ASCII hyphens in the handbook source')
    styles = getSampleStyleSheet()
    styles.add(ParagraphStyle('GuideTitle', fontName='Helvetica-Bold', fontSize=27,
                              leading=31, textColor=colors.black, spaceAfter=15))
    styles.add(ParagraphStyle('Chapter', fontName='Helvetica-Bold', fontSize=19,
                              leading=23, textColor=colors.black, spaceAfter=12,
                              keepWithNext=True))
    styles.add(ParagraphStyle('Topic', fontName='Helvetica-Bold', fontSize=12,
                              leading=15, textColor=colors.black, spaceBefore=10,
                              spaceAfter=5, keepWithNext=True))
    styles.add(ParagraphStyle('GuideBody', fontName='Helvetica', fontSize=11,
                              leading=14.5, textColor=colors.black, spaceAfter=8,
                              allowWidows=0, allowOrphans=0))
    styles.add(ParagraphStyle('GuideBullet', parent=styles['GuideBody'],
                              leftIndent=12, firstLineIndent=-10, spaceAfter=6))
    styles.add(ParagraphStyle('GuideCell', fontName='Helvetica', fontSize=10,
                              leading=12.5, textColor=colors.black))
    styles.add(ParagraphStyle('GuideCellHead', parent=styles['GuideCell'],
                              fontName='Helvetica-Bold'))
    styles.add(ParagraphStyle('GuideSource', fontName='Helvetica', fontSize=9,
                              leading=11.5, textColor=colors.HexColor('#4a4a4a'),
                              spaceBefore=6, spaceAfter=7))
    width = letter[0] - 94

    def footer(canvas, doc):
        canvas.saveState()
        canvas.setFont('Helvetica', 8)
        canvas.setFillColor(colors.HexColor('#555555'))
        canvas.drawString(47, 24, 'Civilizations | Living guide 0.1 | 20 October 2026')
        canvas.drawRightString(letter[0] - 47, 24, str(doc.page))
        if doc.page > 1:
            canvas.setFillColor(colors.black)
            canvas.drawString(47, letter[1] - 27, 'PLAYER AND SERVER GUIDE')
        canvas.restoreState()

    doc = Handbook(str(output), pagesize=letter, leftMargin=47, rightMargin=47,
                   topMargin=48, bottomMargin=44, title='Civilizations Player and Server Guide',
                   author='Civilizations project', subject='Living player and administrator handbook')
    doc.addPageTemplates(PageTemplate(id='guide', frames=[Frame(
        47, 44, width, letter[1] - 92, leftPadding=0, rightPadding=0,
        topPadding=0, bottomPadding=0)], onPage=footer))
    toc = TableOfContents()
    toc.levelStyles = [ParagraphStyle('ContentsEntry', fontName='Helvetica',
                                      fontSize=11, leading=15, spaceAfter=6,
                                      textColor=colors.black, leftIndent=0,
                                      firstLineIndent=0)]
    story = []
    lines = text.splitlines()
    index = 0
    while index < len(lines):
        line = lines[index].strip()
        if not line or line.startswith('<!--'):
            index += 1
            continue
        if line.startswith('# '):
            story.append(Paragraph(inline(line[2:]), styles['GuideTitle']))
        elif line == '## Chapter list':
            story.extend([PageBreak(), Paragraph('Chapter list', styles['GuideTitle']),
                          Paragraph('Choose a chapter in this PDF or use the bookmarks panel. '
                                    'Each feature chapter identifies its current implementation status.',
                                    styles['GuideBody']), Spacer(1, 8), toc])
            # A human-readable list may also follow the marker in the Markdown.
            index += 1
            while index < len(lines) and not lines[index].startswith('## '):
                index += 1
            continue
        elif line.startswith('## '):
            story.extend([PageBreak(), Paragraph(inline(line[3:]), styles['Chapter'])])
        elif line.startswith('### '):
            story.append(Paragraph(inline(line[4:]), styles['Topic']))
        elif line.startswith('|'):
            rows = []
            while index < len(lines) and lines[index].strip().startswith('|'):
                cells = [s.strip() for s in lines[index].strip().strip('|').split('|')]
                if not all(re.fullmatch(r':?-+:?', c) for c in cells):
                    rows.append(cells)
                index += 1
            if not rows or any(len(row) != 2 for row in rows):
                raise ValueError('Handbook tables must have two columns')
            table_data = [[Paragraph(inline(cell), styles['GuideCellHead' if n == 0 else 'GuideCell'])
                           for cell in row] for n, row in enumerate(rows)]
            first = 0.46 if rows[0][0] == 'Command' else 0.37
            table = Table(table_data, colWidths=[width * first, width * (1-first)],
                          repeatRows=1, hAlign='LEFT')
            table.setStyle(TableStyle([
                ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#eeeeee')),
                ('VALIGN', (0, 0), (-1, -1), 'TOP'),
                ('LEFTPADDING', (0, 0), (-1, -1), 7),
                ('RIGHTPADDING', (0, 0), (-1, -1), 7),
                ('TOPPADDING', (0, 0), (-1, -1), 6),
                ('BOTTOMPADDING', (0, 0), (-1, -1), 6),
                ('LINEBELOW', (0, 0), (-1, 0), 0.6, colors.HexColor('#999999')),
                ('LINEBELOW', (0, 1), (-1, -1), 0.25, colors.HexColor('#dddddd')),
            ]))
            story.extend([table, Spacer(1, 8)])
            continue
        elif line.startswith('- '):
            story.append(Paragraph('- ' + inline(line[2:]), styles['GuideBullet']))
        else:
            parts = [line]
            while index + 1 < len(lines) and lines[index+1].strip() and not lines[index+1].startswith(('#', '|', '- ', '<!--')):
                index += 1
                parts.append(lines[index].strip())
            body = ' '.join(parts)
            style = styles['GuideSource'] if body.startswith(('Sources:', 'Related chapters:', 'Source:')) else styles['GuideBody']
            story.append(Paragraph(inline(body), style))
        index += 1
    doc.multiBuild(story)
    print(output)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, default=SOURCE)
    parser.add_argument('--output', type=Path, default=OUTPUT)
    args = parser.parse_args()
    build(args.source, args.output)
