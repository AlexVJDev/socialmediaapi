"""
Печатает дерево пакетов Java-модуля с типами классов в каждой папке.

Используется для docs/md/shema.md (запуск из корня проекта).

Использование:
    python3 -I scripts/gen-schema.py <путь к пакету модуля>            # только папки + типы классов
    python3 -I scripts/gen-schema.py <путь к пакету модуля> --classes  # + каждый класс отдельно (для анализа)

Пример:
    python3 -I scripts/gen-schema.py src/main/java/ru/job4j/socialmediaapi/friendships
"""
import os, re, sys
root = sys.argv[1]
WITH_CLASSES = "--classes" in sys.argv[2:]
ANN = ["RestControllerAdvice","RestController","Controller","Service","Component","Repository","Configuration",
       "ConfigurationProperties","Entity","Embeddable","Mapper","Converter","WorkflowInterface","ActivityInterface",
       "ApplicationModule","NamedInterface","Scheduled","ApplicationModuleListener","EventListener","Transactional","FeignClient"]
def info(path):
    src = open(path, encoding="utf-8").read()
    if path.endswith("package-info.java"):
        anns = [a for a in ANN if re.search(r'@(?:[\w.]+\.)?'+a+r'\b', src)]
        return "package-info" + (" (" + ", ".join("@"+a for a in anns) + ")" if anns else "")
    code = re.sub(r'/\*.*?\*/', '', src, flags=re.S)
    code = re.sub(r'//[^\n]*', '', code)
    m = re.search(r'^\s*((?:public|protected|private|abstract|final|sealed|non-sealed|static|\s)*)\s*(@interface|interface|record|enum|class)\s+(\w+)', code, re.M)
    kind = "?"
    if m:
        mods, kw = m.group(1), m.group(2)
        kind = {"@interface":"annotation","interface":"interface","record":"record","enum":"enum","class":"class"}[kw]
        if kw == "class" and "abstract" in mods: kind = "abstract class"
        head = code[:m.start()]
    else:
        head = code
    anns = [a for a in ANN if re.search(r'@(?:[\w.]+\.)?'+a+r'\b', head)]
    # method-level markers
    extra = [a for a in ["Scheduled","ApplicationModuleListener","EventListener","WorkflowMethod","ActivityMethod"]
             if a not in anns and re.search(r'@(?:[\w.]+\.)?'+a+r'\b', code)]
    impl = ""
    if m:
        tail = code[m.end():code.find("{", m.end())]
        e = re.search(r'extends\s+([\w<>,.\s?]+?)(?=implements|$)', tail.strip()+" ")
        i = re.search(r'implements\s+([\w<>,.\s?]+)', tail)
        parts=[]
        if e: parts.append("extends " + re.sub(r'\s+',' ',e.group(1).strip()))
        if i: parts.append("implements " + re.sub(r'\s+',' ',i.group(1).strip()))
        impl = "; ".join(parts).replace("uz.kapitalbank.integration.orgstructure.","")
    s = kind
    if anns: s += " " + " ".join("@"+a for a in anns)
    if impl: s += " — " + impl
    if extra: s += " [" + ", ".join("@"+a for a in extra) + "]"
    return s

from collections import Counter
def short(i):
    # тип + аннотации, без конкретных extends/implements
    base=i.split(" — ")[0].split(" [")[0]
    if "JpaRepository" in i: base+=" (JpaRepository)"
    elif " — " in i and "Exception" not in i and "Port" not in i and "Store" not in i and "Launcher" not in i:
        pass
    if "implements" in i and any(x in i for x in ("Port","Store","Launcher")): base+=" (реализация порта)"
    if "Exception" in i.split(" — ")[-1] or "extends PortException" in i: base+=" (исключение)"
    if "[@" in i: base+=" "+i[i.index("["):]
    return base
out=[]
def walk(d, prefix=""):
    dirs=sorted(e for e in os.listdir(d) if os.path.isdir(os.path.join(d,e)))
    for idx,name in enumerate(dirs):
        last=idx==len(dirs)-1
        sub=os.path.join(d,name)
        files=sorted(f for f in os.listdir(sub) if f.endswith(".java"))
        c=Counter(short(info(os.path.join(sub,f))) for f in files)
        empty=not any(f.endswith(".java") for _,_,fs in os.walk(sub) for f in fs)
        desc=", ".join(k for k,_ in c.most_common())
        tag="  ‹пустая папка›" if empty else ("  ‹"+desc+"›" if desc else "")
        out.append(prefix+("└── " if last else "├── ")+name+"/"+tag)
        child=prefix+("    " if last else "│   ")
        if WITH_CLASSES:
            subdirs=[e for e in os.listdir(sub) if os.path.isdir(os.path.join(sub,e))]
            for j,f in enumerate(files):
                lf = j==len(files)-1 and not subdirs
                out.append(child+("└── " if lf else "├── ")+f[:-5]+"  ‹"+info(os.path.join(sub,f))+"›")
        walk(sub, child)
walk(root)
print("\n".join(out))
