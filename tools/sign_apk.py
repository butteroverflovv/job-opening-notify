#!/usr/bin/env python3
"""Sign an unsigned APK with an owner-held key; never print the private config."""
import argparse,json,pathlib,subprocess,tempfile,os
p=argparse.ArgumentParser()
p.add_argument('--input',required=True);p.add_argument('--output',required=True)
p.add_argument('--signer',required=True);p.add_argument('--credentials',required=True)
a=p.parse_args()
config_path=pathlib.Path(a.credentials).resolve()
config=json.loads(config_path.read_text())
key=(config_path.parent/config['keystore']).resolve()
with tempfile.TemporaryDirectory(prefix='history-apk-pass-') as private:
    password=pathlib.Path(private)/'store-password';password.write_text(config['password']+'\n');os.chmod(password,0o600)
    key_password=pathlib.Path(private)/'key-password';key_password.write_text(config['password']+'\n');os.chmod(key_password,0o600)
    subprocess.run(['java','-jar',a.signer,'sign','--ks',str(key),'--ks-key-alias',config['alias'],'--ks-pass','file:'+str(password),'--key-pass','file:'+str(key_password),'--out',a.output,a.input],check=True)
subprocess.run(['java','-jar',a.signer,'verify','--verbose','--print-certs',a.output],check=True)
