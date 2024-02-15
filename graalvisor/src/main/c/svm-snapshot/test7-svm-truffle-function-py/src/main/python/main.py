import shutil

def compression(path):
    return shutil.make_archive(path, 'zip', path)

def main(args):
    try:
        return {"result": compression(args)}
    except Exception as e:
        return {"result": str(e)}
