import os

files = [
    'src/main/resources/templates/admin/users.html',
    'src/main/resources/templates/admin/users-import.html',
    'src/main/resources/templates/admin/settings.html',
    'src/main/resources/templates/profile.html',
    'src/main/resources/templates/fragments/footer.html'
]

def replace_theme(content):
    content = content.replace('bg-[#0b0f19]', 'bg-slate-50')
    content = content.replace('text-slate-100', 'text-slate-900')
    content = content.replace('bg-slate-900/60', 'bg-white')
    content = content.replace('bg-slate-900/80', 'bg-slate-100')
    content = content.replace('bg-slate-900/30', 'bg-slate-50')
    content = content.replace('bg-slate-900/95', 'bg-white')
    content = content.replace('bg-slate-950/20', 'bg-white')
    content = content.replace('bg-slate-950/50', 'bg-white')
    content = content.replace('bg-slate-950', 'bg-white')
    content = content.replace('border-slate-800', 'border-slate-200')
    
    content = content.replace('text-slate-200', 'text-slate-800')
    content = content.replace('text-slate-300', 'text-slate-700')
    content = content.replace('text-slate-400', 'text-slate-500')
    content = content.replace('placeholder-slate-500', 'placeholder-slate-400')
    content = content.replace('divide-slate-800/60', 'divide-slate-200')
    
    # Replace text-white to dark text, but then fix buttons
    content = content.replace('text-white', 'text-slate-900')
    
    # Restore text-white on primary colored buttons/elements
    content = content.replace('bg-fpt-orange hover:bg-fpt-orangeHover text-slate-900', 'bg-fpt-orange hover:bg-fpt-orangeHover text-white')
    content = content.replace('bg-fpt-orange text-slate-900', 'bg-fpt-orange text-white')
    content = content.replace('bg-blue-600 hover:bg-blue-700 text-slate-900', 'bg-blue-600 hover:bg-blue-700 text-white')
    content = content.replace('bg-emerald-600 hover:bg-emerald-700 text-slate-900', 'bg-emerald-600 hover:bg-emerald-700 text-white')
    content = content.replace('bg-rose-600 hover:bg-rose-700 text-slate-900', 'bg-rose-600 hover:bg-rose-700 text-white')
    content = content.replace('hover:text-slate-900 pb-2', 'hover:text-slate-900 pb-2') # Tab hover
    
    return content

for path in files:
    if os.path.exists(path):
        with open(path, 'r', encoding='utf-8') as f:
            c = f.read()
        new_c = replace_theme(c)
        with open(path, 'w', encoding='utf-8') as f:
            f.write(new_c)
        print("Updated " + path)
    else:
        print("File not found: " + path)
