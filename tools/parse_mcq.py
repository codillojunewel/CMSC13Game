import docx, json, re, sys

SRC = sys.argv[1]
OUT = sys.argv[2]

d = docx.Document(SRC)
paras = []
for p in d.paragraphs:
    mono = any((r.font.name or '') in ('Consolas', 'Courier New') for r in p.runs)
    paras.append((p.text.replace(' ', ' ').rstrip(), mono))

TYPE_NAMES = {'A': 'Output Prediction', 'B': 'Match the Output', 'C': 'Complete the Code'}

questions = []
module = None
module_no = None
language = None
category = None
cur = None
state = None  # prompt | code | expected | choices | done


def finish():
    global cur
    if cur is None:
        return
    for k in ('code', 'expectedOutput'):
        txt = '\n'.join(cur[k]).strip('\n')
        # strip the single-space "blank" lines Word uses
        txt = '\n'.join('' if l.strip() == '' else l for l in txt.split('\n'))
        cur[k] = txt
    cur['prompt'] = ' '.join(cur['prompt']).strip()
    cur['choices'] = [c.strip('\n') for c in cur['choices']]
    questions.append(cur)
    cur = None


for text, mono in paras:
    t = text.strip()
    m = re.match(r'^Module (\d+): (.+)$', t)
    if m and not mono:
        finish(); category = 'Theoretical'; module_no = int(m.group(1)); module = m.group(2); language = None
        continue
    m = re.match(r'^(C|Java|Python|JavaScript) \((\d+) Questions\)$', t)
    if m and not mono:
        finish(); category = 'Programming'; language = m.group(1); module = None; module_no = None
        continue
    if category is None:
        continue
    m = re.match(r'^(\d+)\.\s+(.*)$', t)
    if m and not mono and (state in (None, 'done')):
        finish()
        cur = {'category': category, 'module': module, 'moduleNo': module_no, 'language': language,
               'typeCode': None, 'type': 'Concept', 'prompt': [], 'code': [], 'expectedOutput': [],
               'choices': [], 'correctIndex': -1, 'rationale': ''}
        rest = m.group(2)
        mt = re.match(r'^Type ([ABC])_(\d+)$', rest)
        if mt:
            cur['typeCode'] = f'{mt.group(1)}_{mt.group(2)}'
            cur['type'] = TYPE_NAMES[mt.group(1)]
        else:
            cur['prompt'].append(rest)
        state = 'prompt'
        continue
    if cur is None:
        continue
    if not mono and t == 'Choices:':
        state = 'choices'; continue
    if not mono and t == 'Expected output:':
        state = 'expected'; continue
    ma = re.match(r'^Answer:\s*([A-D])', t)
    if ma and not mono:
        cur['correctIndex'] = 'ABCD'.index(ma.group(1)); state = 'done'; continue
    mr = re.match(r'^Rationale:\s*(.*)$', t)
    if mr and not mono:
        cur['rationale'] = mr.group(1).strip(); state = 'done'; continue
    if state == 'done':
        if t and cur['rationale']:
            cur['rationale'] += ' ' + t
        continue
    mc = re.match(r'^([A-D])\.\s?(.*)$', text.lstrip())
    if mc and (state == 'choices' or (state in ('prompt', 'code') and not mono and len(cur['choices']) == 'ABCD'.index(mc.group(1)))):
        if 'ABCD'.index(mc.group(1)) == len(cur['choices']):
            state = 'choices'
            cur['choices'].append(mc.group(2))
            continue
    if state == 'choices' and cur['choices']:
        cur['choices'][-1] += '\n' + text  # continuation of multi-line choice
        continue
    if mono:
        if state == 'expected':
            cur['expectedOutput'].append(text)
        else:
            state = 'code'
            cur['code'].append(text)
    else:
        if t:
            if state == 'code':
                # prose after code (e.g. note) -> keep in prompt
                cur['prompt'].append(t)
            else:
                cur['prompt'].append(t)
finish()

problems = []
for i, q in enumerate(questions, 1):
    q['id'] = i
    if len(q['choices']) != 4 or q['correctIndex'] < 0 or not q['prompt']:
        problems.append((i, q['prompt'][:60], len(q['choices']), q['correctIndex']))

json.dump(questions, open(OUT, 'w'), indent=2, ensure_ascii=False)
print('total', len(questions))
from collections import Counter
print(Counter((q['category'], q['module'] or q['language']) for q in questions))
print('problems', problems)
