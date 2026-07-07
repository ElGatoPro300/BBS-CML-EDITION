import json
import os
import glob

# -----------------------------------------------------------------------------
# Console Command: python update_langs.py
# -----------------------------------------------------------------------------

# Path to strings directory
strings_dir = r"C:\Users\PC\Documents\GitHub\bbs-mod\src\client\resources\assets\bbs\assets\strings"

# New keys and English values
new_keys_en = {
    "bbs.example": "Paste the translation keys here, in this case for English.",
}

# -----------------------------------------------------------------------------
# Here you place the translations for your language,
# or you can tell an AI to create the translations
# for all languages ​​using the structure of this file.
# 
# This file also helps to organize translations with jumbled lines of code. Just remember,
# if you're going to use this file for only a few languages,
# delete the translation keys you won't be using to avoid unwanted translations.
# -----------------------------------------------------------------------------

# Spanish (es_es)
new_keys_es = {
    "bbs.example": "Pega las claves de traducción aquí, en este caso para Español."
}

# Portuguese (pt_br, pt_pt)
new_keys_pt = {
    "bbs.example": "Cole as chaves de tradução aqui, neste caso para Português."
}

# French (fr_fr)
new_keys_fr = {
    "bbs.example": "Collez les clés de traduction ici, dans ce cas pour le Français."
}

# German (de_de)
new_keys_de = {
    "bbs.example": "Fügen Sie hier die Übersetzungsschlüssel ein, in diesem Fall für Deutsch."
}

# Russian (ru_ru)
new_keys_ru = {
    "bbs.example": "Вставьте ключи перевода сюда, в данном случае для Русского."
}

# Simplified Chinese (zh_cn)
new_keys_cn = {
    "bbs.example": "在此处粘贴翻译键，在这种情况下是中文。"
}

# Polish (pl_pl)
new_keys_pl = {
    "bbs.example": "Wklej tutaj klucze tłumaczenia, w tym przypadku dla języka Polskiego."
}

# Turkish (tr_tr)
new_keys_tr = {
    "bbs.example": "Çeviri anahtarlarını buraya yapıştırın, bu durumda Türkçe için."
}

# Korean (ko_kr)
new_keys_kr = {
    "bbs.example": "여기에 번역 키를 붙여넣으세요. 이 경우에는 한국어입니다."
}

# Vietnamese (vi_vn)
# new_keys_vi = {
#    "bbs.example": "Dán các khóa dịch vào đây, trong trường hợp này là Tiếng Việt."
# } Diobede I'll translate it. :angry:

# Ukrainian (uk_ua)
new_keys_uk = {
    "bbs.example": "Вставте ключі перекладу сюди, в даному випадку для Української."
}

# Indonesian (id_id)
new_keys_id = {
    "bbs.example": "Tempel kunci terjemahan di sini, dalam hal ini untuk Bahasa Indonesia."
}

# Traditional Chinese (zh_tw)
new_keys_tw = {
    "bbs.example": "在此處貼上翻譯鍵，在本例中為中文（繁體）。"
}

# Arabic (ar_ar)
new_keys_ar = {
    "bbs.example": "ألصق مفاتيح الترجمة هنا، في هذه الحالة للغة العربية."
}

# Hungarian (hu_hu)
new_keys_hu = {
    "bbs.example": "Illessze be ide a fordítási kulcsokat, ebben az esetben Magyar nyelvhez."
}

# Thai (th_th)
new_keys_th = {
    "bbs.example": "วางคีย์การแปลที่นี่ ในกรณีนี้สำหรับภาษาไทย"
}

# Urdu (ur_pk)
new_keys_ur = {
    "bbs.example": "ترجمہ کی چابیاں یہاں پیسٹ کریں، اس صورت میں اردو کے لیے۔"
}

# -----------------------------------------------------------------------------
# Logic to select dictionary
# -----------------------------------------------------------------------------

def update_file(filepath):
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            data = json.load(f)
        
        filename = os.path.basename(filepath)
        
        # Select appropriate dictionary based on filename
        if filename == "es_es.json":
            updates = new_keys_es
        elif filename == "en_us.json":
            updates = new_keys_en
        # European
        elif filename == "fr_fr.json":
            updates = new_keys_fr
        elif filename == "de_de.json":
            updates = new_keys_de
        elif filename == "pt_br.json" or filename == "pt_pt.json":
            updates = new_keys_pt
        elif filename == "pl_pl.json":
            updates = new_keys_pl
        elif filename == "hu_hu.json":
            updates = new_keys_hu
        # Asian
        elif filename == "zh_cn.json":
            updates = new_keys_cn
        elif filename == "zh_tw.json":
            updates = new_keys_tw
        elif filename == "ko_kr.json":
            updates = new_keys_kr
        elif filename == "vi_vn.json":
            updates = new_keys_vi
        elif filename == "th_th.json":
            updates = new_keys_th
        elif filename == "id_id.json":
            updates = new_keys_id
        # Cyrillic
        elif filename == "ru_ru.json":
            updates = new_keys_ru
        elif filename == "uk_ua.json":
            updates = new_keys_uk
        # Middle Eastern / RTL
        elif filename == "tr_tr.json":
            updates = new_keys_tr
        elif filename == "ar_ar.json":
            updates = new_keys_ar
        elif filename == "ur_pk.json":
            updates = new_keys_ur
        else:
            # Fallback to English for any other unmapped files (e.g. he_il.json)
            updates = new_keys_en
            
        # Update data if key is missing
        modified = False
        for key, value in updates.items():
            if key not in data:
                data[key] = value
                modified = True
            elif filename == "en_us.json" and data[key] != value:
                 # Ensure English file matches exact values if they differ
                 data[key] = value
                 modified = True
        
        if modified or True: # Always write to ensure sorting
            # Sort keys to ensure correct order
            sorted_data = dict(sorted(data.items()))
            
            with open(filepath, 'w', encoding='utf-8') as f:
                json.dump(sorted_data, f, indent=4, ensure_ascii=False)
            print(f"Updated {filename}")
            
    except Exception as e:
        print(f"Error updating {filepath}: {e}")

# Process all json files
json_files = glob.glob(os.path.join(strings_dir, "*.json"))
for json_file in json_files:
    update_file(json_file)
