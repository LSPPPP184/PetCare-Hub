"""Dùng nguyên dependency/plugin của pom gốc; chuyển mọi output build vào docs/."""
from pathlib import Path
import xml.etree.ElementTree as E
root = Path(__file__).resolve().parents[2]
ns = 'http://maven.apache.org/POM/4.0.0'
E.register_namespace('',ns)
E.register_namespace('xsi','http://www.w3.org/2001/XMLSchema-instance')
tree = E.parse(root/'pom.xml')
project = tree.getroot()
build = project.find('{'+ns+'}build')
def add(parent,tag,value=None):
    e = E.SubElement(parent,'{'+ns+'}'+tag)
    e.text = value
    return e
add(build,'directory',(root/'docs/_verification/build').as_posix())
add(build,'sourceDirectory',(root/'src/main/java').as_posix())
add(build,'testSourceDirectory',(root/'src/test/java').as_posix())
add(add(add(build,'resources'),'resource'),'directory',(root/'src/main/resources').as_posix())
add(add(add(build,'testResources'),'testResource'),'directory',(root/'src/test/resources').as_posix())
for plugin in build.find('{'+ns+'}plugins'):
    if plugin.find('{'+ns+'}artifactId').text == 'maven-surefire-plugin':
        add(plugin.find('{'+ns+'}configuration'),'workingDirectory',root.as_posix())
E.indent(tree)
tree.write(root/'docs/_verification/pom.xml',encoding='utf-8',xml_declaration=True)
print('Prepared docs/_verification/pom.xml; build output stays in docs/_verification/build.')
