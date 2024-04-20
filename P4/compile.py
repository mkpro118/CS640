import os

files = filter(lambda x: x.endswith('.java'), os.listdir())

os.system(f'javac -d bin {" ".join(files)}')
