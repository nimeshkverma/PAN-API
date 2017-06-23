import subprocess
import uuid
import requests
import settings


class PAN(object):
    """
        PAN is a class to get the details for the given PAN.
        input:
            pan:
                type: string
                description: the pan for which the details will be fetched

    """

    def __init__(self, pan):
        self.pwd = self.__pwd()
        self.sig_file_name = self.__sig_file_name()
        self.jks_file_name = self.__jks_file_name()
        self.pan = pan
        self.signature_inputs = self.__signature_inputs()
        self.commands = self.__commands()
        self.signature = self.__signature()
        self.pan_raw_data = self.__pan_raw_data()
        self.pan_key_list = ['return_code', 'PAN', 'PAN_status', 'last_name',
                             'first_name', 'middle_name', 'PAN_title', 'last_update_date', ]
        self.pan_detail_dict = self.__pan_detail_dict()

    def __pwd(self):
        subprocess_pwd = subprocess.check_output('pwd')
        return subprocess_pwd.split('\n')[0]

    def __sig_file_name(self):
        return '{name}.sig'.format(name=uuid.uuid4().hex)

    def __jks_file_name(self):
        return '{name}.jks'.format(name=uuid.uuid4().hex)

    def __signature_inputs(self):
        return '{vendor_id}^{pan}'.format(vendor_id=settings.VENDOR_ID, pan=self.pan).upper()

    def __commands(self):
        commands = {
            'compile_SignatureGenerator': 'javac -cp {pwd}/bcmail-jdk16-1.44.jar:{pwd}/bcprov-jdk16-1.44.jar:{pwd}/ SignatureGenerator.java'.format(pwd=self.pwd),
            'run_SignatureGenerator': 'java -cp {pwd}/bcmail-jdk16-1.44.jar:{pwd}/bcprov-jdk16-1.44.jar:{pwd}/ SignatureGenerator {pfx_file} {password} {jks_file} {inputs} {sig_file}'.format(pwd=self.pwd, pfx_file=settings.PFX_FILE, password=settings.PFX_FILE_PASSWORD, jks_file=self.jks_file_name, inputs=self.signature_inputs, sig_file=self.sig_file_name),
            'sig_file_path': '{pwd}/{sig_file}'.format(pwd=self.pwd, sig_file=self.sig_file_name),
            'remove_sig_file': 'rm {pwd}/{sig_file}'.format(pwd=self.pwd, sig_file=self.sig_file_name),
            'remove_jks_file': 'rm {pwd}/{jks_file}'.format(pwd=self.pwd, jks_file=self.jks_file_name),
        }
        print commands
        return commands

    def __file_content(self, file_path):
        return open(file_path, 'r').read()

    def __execute_commands(self, command_key_list):
        for command_key in command_key_list:
            subprocess.call(self.commands[command_key], shell=True)

    def __signature(self):
        signature = ''
        self.__execute_commands(
            ['compile_SignatureGenerator', 'run_SignatureGenerator'])
        signature = self.__file_content(self.commands['sig_file_path'])
        self.__execute_commands(['remove_sig_file', 'remove_jks_file'])
        return signature

    def __pan_raw_data(self):
        headers = {
            'content-type': 'application/x-www-form-urlencoded'
        }
        params = {
            'data': self.signature_inputs,
            'signature': self.signature
        }
        requests_data = requests.post(
            settings.PAN_SERVER_URL, data=params, headers=headers, verify=False)
        return requests_data.text

    def __pan_detail_dict(self):
        pan_dict = {
            'return_code': None,
            'PAN': None,
            'PAN_status': None,
            'last_name': None,
            'first_name': None,
            'middle_name': None,
            'PAN_title': None,
            'last_update_date': None,
        }
        pan_raw_list = self.pan_raw_data.split('^')
        for index in xrange(0, len(self.pan_key_list)):
            pan_dict[self.pan_key_list[index]] = pan_raw_list[index]
        return pan_dict
