package jcifs.dcerpc.msrpc;

import jcifs.dcerpc.*;
import jcifs.dcerpc.ndr.*;

public class netlogon {

    public static String getSyntax() {
        return "12345678-1234-abcd-ef00-01234567cffb:1.0";
    }

    public static class lsa_String extends NdrObject {

        public short length;
        public short size;
        public short[] string;

        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_short(length);
            _dst.enc_ndr_short(size);
            _dst.enc_ndr_referent(string, 1);

            if (string != null) {
                _dst = _dst.deferred;
                int _stringl = length/2;
                int _strings = size/2;
                _dst.enc_ndr_long(_strings);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_stringl);
                int _stringi = _dst.index;
                _dst.advance(2 * _stringl);

                _dst = _dst.derive(_stringi);
                for (int _i = 0; _i < _stringl; _i++) {
                    _dst.enc_ndr_short(string[_i]);
                }
            }
        }
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            length = (short)_src.dec_ndr_short();
            size = (short)_src.dec_ndr_short();
            int _stringp = _src.dec_ndr_long();

            if (_stringp != 0) {
                _src = _src.deferred;
                int _strings = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _stringl = _src.dec_ndr_long();
                int _stringi = _src.index;
                _src.advance(2 * _stringl);

                if (string == null) {
                    if (_strings < 0 || _strings > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    string = new short[_strings];
                }
                _src = _src.derive(_stringi);
                for (int _i = 0; _i < _stringl; _i++) {
                    string[_i] = (short)_src.dec_ndr_short();
                }
            }
        }
    }
    public static class lsa_StringLarge extends NdrObject {

        public short length;
        public short size;
        public short[] string;

        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_short(length);
            _dst.enc_ndr_short(size);
            _dst.enc_ndr_referent(string, 1);

            if (string != null) {
                _dst = _dst.deferred;
                int _stringl = length/2;
                int _strings = size/2;
                _dst.enc_ndr_long(_strings);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_stringl);
                int _stringi = _dst.index;
                _dst.advance(2 * _stringl);

                _dst = _dst.derive(_stringi);
                for (int _i = 0; _i < _stringl; _i++) {
                    _dst.enc_ndr_short(string[_i]);
                }
            }
        }
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            length = (short)_src.dec_ndr_short();
            size = (short)_src.dec_ndr_short();
            int _stringp = _src.dec_ndr_long();

            if (_stringp != 0) {
                _src = _src.deferred;
                int _strings = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _stringl = _src.dec_ndr_long();
                int _stringi = _src.index;
                _src.advance(2 * _stringl);

                if (string == null) {
                    if (_strings < 0 || _strings > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    string = new short[_strings];
                }
                _src = _src.derive(_stringi);
                for (int _i = 0; _i < _stringl; _i++) {
                    string[_i] = (short)_src.dec_ndr_short();
                }
            }
        }
    }
    public static class lsa_Strings extends NdrObject {

        public int count;
        public lsa_String[] names;

        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_long(count);
            _dst.enc_ndr_referent(names, 1);

            if (names != null) {
                _dst = _dst.deferred;
                int _namess = count;
                _dst.enc_ndr_long(_namess);
                int _namesi = _dst.index;
                _dst.advance(8 * _namess);

                _dst = _dst.derive(_namesi);
                for (int _i = 0; _i < _namess; _i++) {
                    names[_i].encode(_dst);
                }
            }
        }
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            count = (int)_src.dec_ndr_long();
            int _namesp = _src.dec_ndr_long();

            if (_namesp != 0) {
                _src = _src.deferred;
                int _namess = _src.dec_ndr_long();
                int _namesi = _src.index;
                _src.advance(8 * _namess);

                if (names == null) {
                    if (_namess < 0 || _namess > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    names = new lsa_String[_namess];
                }
                _src = _src.derive(_namesi);
                for (int _i = 0; _i < _namess; _i++) {
                    if (names[_i] == null) {
                        names[_i] = new lsa_String();
                    }
                    names[_i].decode(_src);
                }
            }
        }
    }
    public static class lsa_AsciiString extends NdrObject {

        public short length;
        public short size;
        public byte[] string;

        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_short(length);
            _dst.enc_ndr_short(size);
            _dst.enc_ndr_referent(string, 1);

            if (string != null) {
                _dst = _dst.deferred;
                int _stringl = length;
                int _strings = size;
                _dst.enc_ndr_long(_strings);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_stringl);
                int _stringi = _dst.index;
                _dst.advance(1 * _stringl);

                _dst = _dst.derive(_stringi);
                for (int _i = 0; _i < _stringl; _i++) {
                    _dst.enc_ndr_small(string[_i]);
                }
            }
        }
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            length = (short)_src.dec_ndr_short();
            size = (short)_src.dec_ndr_short();
            int _stringp = _src.dec_ndr_long();

            if (_stringp != 0) {
                _src = _src.deferred;
                int _strings = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _stringl = _src.dec_ndr_long();
                int _stringi = _src.index;
                _src.advance(1 * _stringl);

                if (string == null) {
                    if (_strings < 0 || _strings > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    string = new byte[_strings];
                }
                _src = _src.derive(_stringi);
                for (int _i = 0; _i < _stringl; _i++) {
                    string[_i] = (byte)_src.dec_ndr_small();
                }
            }
        }
    }
    public static class lsa_AsciiStringLarge extends NdrObject {

        public short length;
        public short size;
        public byte[] string;

        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_short(length);
            _dst.enc_ndr_short(size);
            _dst.enc_ndr_referent(string, 1);

            if (string != null) {
                _dst = _dst.deferred;
                int _stringl = length;
                int _strings = size;
                _dst.enc_ndr_long(_strings);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_stringl);
                int _stringi = _dst.index;
                _dst.advance(1 * _stringl);

                _dst = _dst.derive(_stringi);
                for (int _i = 0; _i < _stringl; _i++) {
                    _dst.enc_ndr_small(string[_i]);
                }
            }
        }
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            length = (short)_src.dec_ndr_short();
            size = (short)_src.dec_ndr_short();
            int _stringp = _src.dec_ndr_long();

            if (_stringp != 0) {
                _src = _src.deferred;
                int _strings = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _stringl = _src.dec_ndr_long();
                int _stringi = _src.index;
                _src.advance(1 * _stringl);

                if (string == null) {
                    if (_strings < 0 || _strings > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    string = new byte[_strings];
                }
                _src = _src.derive(_stringi);
                for (int _i = 0; _i < _stringl; _i++) {
                    string[_i] = (byte)_src.dec_ndr_small();
                }
            }
        }
    }
    public static class dom_sid extends NdrObject {

        public byte sid_rev_num;
        public byte num_auths;
        public byte[] id_auth;
        public int[] sub_auths;

        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_small(sid_rev_num);
            _dst.enc_ndr_small(num_auths);
            int _id_auths = 6;
            int _id_authi = _dst.index;
            _dst.advance(1 * _id_auths);
            int _sub_authss = num_auths;
            int _sub_authsi = _dst.index;
            _dst.advance(4 * _sub_authss);

            _dst = _dst.derive(_id_authi);
            for (int _i = 0; _i < _id_auths; _i++) {
                _dst.enc_ndr_small(id_auth[_i]);
            }
            _dst = _dst.derive(_sub_authsi);
            for (int _i = 0; _i < _sub_authss; _i++) {
                _dst.enc_ndr_long(sub_auths[_i]);
            }
        }
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            sid_rev_num = (byte)_src.dec_ndr_small();
            num_auths = (byte)_src.dec_ndr_small();
            int _id_auths = 6;
            int _id_authi = _src.index;
            _src.advance(1 * _id_auths);
            int _sub_authss = num_auths;
            int _sub_authsi = _src.index;
            _src.advance(4 * _sub_authss);

            if (id_auth == null) {
                if (_id_auths < 0 || _id_auths > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                id_auth = new byte[_id_auths];
            }
            _src = _src.derive(_id_authi);
            for (int _i = 0; _i < _id_auths; _i++) {
                id_auth[_i] = (byte)_src.dec_ndr_small();
            }
            if (sub_auths == null) {
                if (_sub_authss < 0 || _sub_authss > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                sub_auths = new int[_sub_authss];
            }
            _src = _src.derive(_sub_authsi);
            for (int _i = 0; _i < _sub_authss; _i++) {
                sub_auths[_i] = (int)_src.dec_ndr_long();
            }
        }
    }
    public static class netr_LogonUasLogon extends DcerpcMessage {

        public int getOpnum() { return 0; }

        public int retval;

        public netr_LogonUasLogon() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_LogonUasLogoff extends DcerpcMessage {

        public int getOpnum() { return 1; }

        public int retval;

        public netr_LogonUasLogoff() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_IdentityInfo extends NdrObject {

        public lsa_String domain_name;
        public int parameter_control;
        public int logon_id_low;
        public int logon_id_high;
        public lsa_String account_name;
        public lsa_String workstation;

        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_short(domain_name.length);
            _dst.enc_ndr_short(domain_name.size);
            _dst.enc_ndr_referent(domain_name.string, 1);
            _dst.enc_ndr_long(parameter_control);
            _dst.enc_ndr_long(logon_id_low);
            _dst.enc_ndr_long(logon_id_high);
            _dst.enc_ndr_short(account_name.length);
            _dst.enc_ndr_short(account_name.size);
            _dst.enc_ndr_referent(account_name.string, 1);
            _dst.enc_ndr_short(workstation.length);
            _dst.enc_ndr_short(workstation.size);
            _dst.enc_ndr_referent(workstation.string, 1);

            if (domain_name.string != null) {
                _dst = _dst.deferred;
                int _domain_name_stringl = domain_name.length/2;
                int _domain_name_strings = domain_name.size/2;
                _dst.enc_ndr_long(_domain_name_strings);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_domain_name_stringl);
                int _domain_name_stringi = _dst.index;
                _dst.advance(2 * _domain_name_stringl);

                _dst = _dst.derive(_domain_name_stringi);
                for (int _i = 0; _i < _domain_name_stringl; _i++) {
                    _dst.enc_ndr_short(domain_name.string[_i]);
                }
            }
            if (account_name.string != null) {
                _dst = _dst.deferred;
                int _account_name_stringl = account_name.length/2;
                int _account_name_strings = account_name.size/2;
                _dst.enc_ndr_long(_account_name_strings);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_account_name_stringl);
                int _account_name_stringi = _dst.index;
                _dst.advance(2 * _account_name_stringl);

                _dst = _dst.derive(_account_name_stringi);
                for (int _i = 0; _i < _account_name_stringl; _i++) {
                    _dst.enc_ndr_short(account_name.string[_i]);
                }
            }
            if (workstation.string != null) {
                _dst = _dst.deferred;
                int _workstation_stringl = workstation.length/2;
                int _workstation_strings = workstation.size/2;
                _dst.enc_ndr_long(_workstation_strings);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_workstation_stringl);
                int _workstation_stringi = _dst.index;
                _dst.advance(2 * _workstation_stringl);

                _dst = _dst.derive(_workstation_stringi);
                for (int _i = 0; _i < _workstation_stringl; _i++) {
                    _dst.enc_ndr_short(workstation.string[_i]);
                }
            }
        }
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            _src.align(4);
            if (domain_name == null) {
                domain_name = new lsa_String();
            }
            domain_name.length = (short)_src.dec_ndr_short();
            domain_name.size = (short)_src.dec_ndr_short();
            int _domain_name_stringp = _src.dec_ndr_long();
            parameter_control = (int)_src.dec_ndr_long();
            logon_id_low = (int)_src.dec_ndr_long();
            logon_id_high = (int)_src.dec_ndr_long();
            _src.align(4);
            if (account_name == null) {
                account_name = new lsa_String();
            }
            account_name.length = (short)_src.dec_ndr_short();
            account_name.size = (short)_src.dec_ndr_short();
            int _account_name_stringp = _src.dec_ndr_long();
            _src.align(4);
            if (workstation == null) {
                workstation = new lsa_String();
            }
            workstation.length = (short)_src.dec_ndr_short();
            workstation.size = (short)_src.dec_ndr_short();
            int _workstation_stringp = _src.dec_ndr_long();

            if (_domain_name_stringp != 0) {
                _src = _src.deferred;
                int _domain_name_strings = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _domain_name_stringl = _src.dec_ndr_long();
                int _domain_name_stringi = _src.index;
                _src.advance(2 * _domain_name_stringl);

                if (domain_name.string == null) {
                    if (_domain_name_strings < 0 || _domain_name_strings > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    domain_name.string = new short[_domain_name_strings];
                }
                _src = _src.derive(_domain_name_stringi);
                for (int _i = 0; _i < _domain_name_stringl; _i++) {
                    domain_name.string[_i] = (short)_src.dec_ndr_short();
                }
            }
            if (_account_name_stringp != 0) {
                _src = _src.deferred;
                int _account_name_strings = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _account_name_stringl = _src.dec_ndr_long();
                int _account_name_stringi = _src.index;
                _src.advance(2 * _account_name_stringl);

                if (account_name.string == null) {
                    if (_account_name_strings < 0 || _account_name_strings > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    account_name.string = new short[_account_name_strings];
                }
                _src = _src.derive(_account_name_stringi);
                for (int _i = 0; _i < _account_name_stringl; _i++) {
                    account_name.string[_i] = (short)_src.dec_ndr_short();
                }
            }
            if (_workstation_stringp != 0) {
                _src = _src.deferred;
                int _workstation_strings = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _workstation_stringl = _src.dec_ndr_long();
                int _workstation_stringi = _src.index;
                _src.advance(2 * _workstation_stringl);

                if (workstation.string == null) {
                    if (_workstation_strings < 0 || _workstation_strings > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    workstation.string = new short[_workstation_strings];
                }
                _src = _src.derive(_workstation_stringi);
                for (int _i = 0; _i < _workstation_stringl; _i++) {
                    workstation.string[_i] = (short)_src.dec_ndr_short();
                }
            }
        }
    }
    public static class netr_ChallengeResponse extends NdrObject {

        public short length;
        public short size;
        public byte[] data;

        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_short(length);
            _dst.enc_ndr_short(size);
            _dst.enc_ndr_referent(data, 1);

            if (data != null) {
                _dst = _dst.deferred;
                int _datal = length;
                int _datas = length;
                _dst.enc_ndr_long(_datas);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_datal);
                int _datai = _dst.index;
                _dst.advance(1 * _datal);

                _dst = _dst.derive(_datai);
                for (int _i = 0; _i < _datal; _i++) {
                    _dst.enc_ndr_small(data[_i]);
                }
            }
        }
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            length = (short)_src.dec_ndr_short();
            size = (short)_src.dec_ndr_short();
            int _datap = _src.dec_ndr_long();

            if (_datap != 0) {
                _src = _src.deferred;
                int _datas = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _datal = _src.dec_ndr_long();
                int _datai = _src.index;
                _src.advance(1 * _datal);

                if (data == null) {
                    if (_datas < 0 || _datas > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    data = new byte[_datas];
                }
                _src = _src.derive(_datai);
                for (int _i = 0; _i < _datal; _i++) {
                    data[_i] = (byte)_src.dec_ndr_small();
                }
            }
        }
    }
    public static class netr_NetworkInfo extends NdrObject {

        public netr_IdentityInfo identity_info;
        public byte[] challenge;
        public netr_ChallengeResponse nt;
        public netr_ChallengeResponse lm;

        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_short(identity_info.domain_name.length);
            _dst.enc_ndr_short(identity_info.domain_name.size);
            _dst.enc_ndr_referent(identity_info.domain_name.string, 1);
            _dst.enc_ndr_long(identity_info.parameter_control);
            _dst.enc_ndr_long(identity_info.logon_id_low);
            _dst.enc_ndr_long(identity_info.logon_id_high);
            _dst.enc_ndr_short(identity_info.account_name.length);
            _dst.enc_ndr_short(identity_info.account_name.size);
            _dst.enc_ndr_referent(identity_info.account_name.string, 1);
            _dst.enc_ndr_short(identity_info.workstation.length);
            _dst.enc_ndr_short(identity_info.workstation.size);
            _dst.enc_ndr_referent(identity_info.workstation.string, 1);
            int _challenges = 8;
            int _challengei = _dst.index;
            _dst.advance(1 * _challenges);
            _dst.enc_ndr_referent(nt, 1);
            _dst.enc_ndr_referent(lm, 1);

            if (identity_info.domain_name.string != null) {
                _dst = _dst.deferred;
                int _identity_info_domain_name_stringl = identity_info.domain_name.length/2;
                int _identity_info_domain_name_strings = identity_info.domain_name.size/2;
                _dst.enc_ndr_long(_identity_info_domain_name_strings);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_identity_info_domain_name_stringl);
                int _identity_info_domain_name_stringi = _dst.index;
                _dst.advance(2 * _identity_info_domain_name_stringl);

                _dst = _dst.derive(_identity_info_domain_name_stringi);
                for (int _i = 0; _i < _identity_info_domain_name_stringl; _i++) {
                    _dst.enc_ndr_short(identity_info.domain_name.string[_i]);
                }
            }
            if (identity_info.account_name.string != null) {
                _dst = _dst.deferred;
                int _identity_info_account_name_stringl = identity_info.account_name.length/2;
                int _identity_info_account_name_strings = identity_info.account_name.size/2;
                _dst.enc_ndr_long(_identity_info_account_name_strings);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_identity_info_account_name_stringl);
                int _identity_info_account_name_stringi = _dst.index;
                _dst.advance(2 * _identity_info_account_name_stringl);

                _dst = _dst.derive(_identity_info_account_name_stringi);
                for (int _i = 0; _i < _identity_info_account_name_stringl; _i++) {
                    _dst.enc_ndr_short(identity_info.account_name.string[_i]);
                }
            }
            if (identity_info.workstation.string != null) {
                _dst = _dst.deferred;
                int _identity_info_workstation_stringl = identity_info.workstation.length/2;
                int _identity_info_workstation_strings = identity_info.workstation.size/2;
                _dst.enc_ndr_long(_identity_info_workstation_strings);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_identity_info_workstation_stringl);
                int _identity_info_workstation_stringi = _dst.index;
                _dst.advance(2 * _identity_info_workstation_stringl);

                _dst = _dst.derive(_identity_info_workstation_stringi);
                for (int _i = 0; _i < _identity_info_workstation_stringl; _i++) {
                    _dst.enc_ndr_short(identity_info.workstation.string[_i]);
                }
            }
            _dst = _dst.derive(_challengei);
            for (int _i = 0; _i < _challenges; _i++) {
                _dst.enc_ndr_small(challenge[_i]);
            }
            if (nt != null) {
                _dst = _dst.deferred;
                nt.encode(_dst);

            }
            if (lm != null) {
                _dst = _dst.deferred;
                lm.encode(_dst);

            }
        }
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            _src.align(4);
            if (identity_info == null) {
                identity_info = new netr_IdentityInfo();
            }
            _src.align(4);
            if (identity_info.domain_name == null) {
                identity_info.domain_name = new lsa_String();
            }
            identity_info.domain_name.length = (short)_src.dec_ndr_short();
            identity_info.domain_name.size = (short)_src.dec_ndr_short();
            int _identity_info_domain_name_stringp = _src.dec_ndr_long();
            identity_info.parameter_control = (int)_src.dec_ndr_long();
            identity_info.logon_id_low = (int)_src.dec_ndr_long();
            identity_info.logon_id_high = (int)_src.dec_ndr_long();
            _src.align(4);
            if (identity_info.account_name == null) {
                identity_info.account_name = new lsa_String();
            }
            identity_info.account_name.length = (short)_src.dec_ndr_short();
            identity_info.account_name.size = (short)_src.dec_ndr_short();
            int _identity_info_account_name_stringp = _src.dec_ndr_long();
            _src.align(4);
            if (identity_info.workstation == null) {
                identity_info.workstation = new lsa_String();
            }
            identity_info.workstation.length = (short)_src.dec_ndr_short();
            identity_info.workstation.size = (short)_src.dec_ndr_short();
            int _identity_info_workstation_stringp = _src.dec_ndr_long();
            int _challenges = 8;
            int _challengei = _src.index;
            _src.advance(1 * _challenges);
            int _ntp = _src.dec_ndr_long();
            int _lmp = _src.dec_ndr_long();

            if (_identity_info_domain_name_stringp != 0) {
                _src = _src.deferred;
                int _identity_info_domain_name_strings = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _identity_info_domain_name_stringl = _src.dec_ndr_long();
                int _identity_info_domain_name_stringi = _src.index;
                _src.advance(2 * _identity_info_domain_name_stringl);

                if (identity_info.domain_name.string == null) {
                    if (_identity_info_domain_name_strings < 0 || _identity_info_domain_name_strings > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    identity_info.domain_name.string = new short[_identity_info_domain_name_strings];
                }
                _src = _src.derive(_identity_info_domain_name_stringi);
                for (int _i = 0; _i < _identity_info_domain_name_stringl; _i++) {
                    identity_info.domain_name.string[_i] = (short)_src.dec_ndr_short();
                }
            }
            if (_identity_info_account_name_stringp != 0) {
                _src = _src.deferred;
                int _identity_info_account_name_strings = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _identity_info_account_name_stringl = _src.dec_ndr_long();
                int _identity_info_account_name_stringi = _src.index;
                _src.advance(2 * _identity_info_account_name_stringl);

                if (identity_info.account_name.string == null) {
                    if (_identity_info_account_name_strings < 0 || _identity_info_account_name_strings > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    identity_info.account_name.string = new short[_identity_info_account_name_strings];
                }
                _src = _src.derive(_identity_info_account_name_stringi);
                for (int _i = 0; _i < _identity_info_account_name_stringl; _i++) {
                    identity_info.account_name.string[_i] = (short)_src.dec_ndr_short();
                }
            }
            if (_identity_info_workstation_stringp != 0) {
                _src = _src.deferred;
                int _identity_info_workstation_strings = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _identity_info_workstation_stringl = _src.dec_ndr_long();
                int _identity_info_workstation_stringi = _src.index;
                _src.advance(2 * _identity_info_workstation_stringl);

                if (identity_info.workstation.string == null) {
                    if (_identity_info_workstation_strings < 0 || _identity_info_workstation_strings > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    identity_info.workstation.string = new short[_identity_info_workstation_strings];
                }
                _src = _src.derive(_identity_info_workstation_stringi);
                for (int _i = 0; _i < _identity_info_workstation_stringl; _i++) {
                    identity_info.workstation.string[_i] = (short)_src.dec_ndr_short();
                }
            }
            if (challenge == null) {
                if (_challenges < 0 || _challenges > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                challenge = new byte[_challenges];
            }
            _src = _src.derive(_challengei);
            for (int _i = 0; _i < _challenges; _i++) {
                challenge[_i] = (byte)_src.dec_ndr_small();
            }
            if (_ntp != 0) {
                if (nt == null) { /* YOYOYO */
                    nt = new netr_ChallengeResponse();
                }
                _src = _src.deferred;
                nt.decode(_src);

            }
            if (_lmp != 0) {
                if (lm == null) { /* YOYOYO */
                    lm = new netr_ChallengeResponse();
                }
                _src = _src.deferred;
                lm.decode(_src);

            }
        }
    }
    public static class netr_UserSessionKey extends NdrObject {

        public byte[] key;

        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(1);
            int _keys = 16;
            int _keyi = _dst.index;
            _dst.advance(1 * _keys);

            _dst = _dst.derive(_keyi);
            for (int _i = 0; _i < _keys; _i++) {
                _dst.enc_ndr_small(key[_i]);
            }
        }
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(1);
            int _keys = 16;
            int _keyi = _src.index;
            _src.advance(1 * _keys);

            if (key == null) {
                if (_keys < 0 || _keys > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                key = new byte[_keys];
            }
            _src = _src.derive(_keyi);
            for (int _i = 0; _i < _keys; _i++) {
                key[_i] = (byte)_src.dec_ndr_small();
            }
        }
    }
    public static class netr_LMSessionKey extends NdrObject {

        public byte[] key;

        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(1);
            int _keys = 8;
            int _keyi = _dst.index;
            _dst.advance(1 * _keys);

            _dst = _dst.derive(_keyi);
            for (int _i = 0; _i < _keys; _i++) {
                _dst.enc_ndr_small(key[_i]);
            }
        }
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(1);
            int _keys = 8;
            int _keyi = _src.index;
            _src.advance(1 * _keys);

            if (key == null) {
                if (_keys < 0 || _keys > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                key = new byte[_keys];
            }
            _src = _src.derive(_keyi);
            for (int _i = 0; _i < _keys; _i++) {
                key[_i] = (byte)_src.dec_ndr_small();
            }
        }
    }
    public static class samr_RidWithAttribute extends NdrObject {

        public int rid;
        public int attributes;

        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_long(rid);
            _dst.enc_ndr_long(attributes);

        }
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            rid = (int)_src.dec_ndr_long();
            attributes = (int)_src.dec_ndr_long();

        }
    }
    public static class samr_RidWithAttributeArray extends NdrObject {

        public int count;
        public samr_RidWithAttribute[] rids;

        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_long(count);
            _dst.enc_ndr_referent(rids, 1);

            if (rids != null) {
                _dst = _dst.deferred;
                int _ridss = count;
                _dst.enc_ndr_long(_ridss);
                int _ridsi = _dst.index;
                _dst.advance(8 * _ridss);

                _dst = _dst.derive(_ridsi);
                for (int _i = 0; _i < _ridss; _i++) {
                    rids[_i].encode(_dst);
                }
            }
        }
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            count = (int)_src.dec_ndr_long();
            int _ridsp = _src.dec_ndr_long();

            if (_ridsp != 0) {
                _src = _src.deferred;
                int _ridss = _src.dec_ndr_long();
                int _ridsi = _src.index;
                _src.advance(8 * _ridss);

                if (rids == null) {
                    if (_ridss < 0 || _ridss > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    rids = new samr_RidWithAttribute[_ridss];
                }
                _src = _src.derive(_ridsi);
                for (int _i = 0; _i < _ridss; _i++) {
                    if (rids[_i] == null) {
                        rids[_i] = new samr_RidWithAttribute();
                    }
                    rids[_i].decode(_src);
                }
            }
        }
    }
    public static class netr_SamBaseInfo extends NdrObject {

        public long last_logon;
        public long last_logoff;
        public long acct_expiry;
        public long last_password_change;
        public long allow_password_change;
        public long force_password_change;
        public lsa_String account_name;
        public lsa_String full_name;
        public lsa_String logon_script;
        public lsa_String profile_path;
        public lsa_String home_directory;
        public lsa_String home_drive;
        public short logon_count;
        public short bad_password_count;
        public int rid;
        public int primary_gid;
        public samr_RidWithAttributeArray groups;
        public int user_flags;
        public netr_UserSessionKey key;
        public lsa_StringLarge logon_server;
        public lsa_StringLarge domain;
        public dom_sid domain_sid;
        public netr_LMSessionKey LMSessKey;
        public int acct_flags;
        public int[] unknown;

        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(8);
            _dst.enc_ndr_hyper(last_logon);
            _dst.enc_ndr_hyper(last_logoff);
            _dst.enc_ndr_hyper(acct_expiry);
            _dst.enc_ndr_hyper(last_password_change);
            _dst.enc_ndr_hyper(allow_password_change);
            _dst.enc_ndr_hyper(force_password_change);
            _dst.enc_ndr_short(account_name.length);
            _dst.enc_ndr_short(account_name.size);
            _dst.enc_ndr_referent(account_name.string, 1);
            _dst.enc_ndr_short(full_name.length);
            _dst.enc_ndr_short(full_name.size);
            _dst.enc_ndr_referent(full_name.string, 1);
            _dst.enc_ndr_short(logon_script.length);
            _dst.enc_ndr_short(logon_script.size);
            _dst.enc_ndr_referent(logon_script.string, 1);
            _dst.enc_ndr_short(profile_path.length);
            _dst.enc_ndr_short(profile_path.size);
            _dst.enc_ndr_referent(profile_path.string, 1);
            _dst.enc_ndr_short(home_directory.length);
            _dst.enc_ndr_short(home_directory.size);
            _dst.enc_ndr_referent(home_directory.string, 1);
            _dst.enc_ndr_short(home_drive.length);
            _dst.enc_ndr_short(home_drive.size);
            _dst.enc_ndr_referent(home_drive.string, 1);
            _dst.enc_ndr_short(logon_count);
            _dst.enc_ndr_short(bad_password_count);
            _dst.enc_ndr_long(rid);
            _dst.enc_ndr_long(primary_gid);
            _dst.enc_ndr_long(groups.count);
            _dst.enc_ndr_referent(groups.rids, 1);
            _dst.enc_ndr_long(user_flags);
            int _key_keys = 16;
            int _key_keyi = _dst.index;
            _dst.advance(1 * _key_keys);
            _dst.enc_ndr_short(logon_server.length);
            _dst.enc_ndr_short(logon_server.size);
            _dst.enc_ndr_referent(logon_server.string, 1);
            _dst.enc_ndr_short(domain.length);
            _dst.enc_ndr_short(domain.size);
            _dst.enc_ndr_referent(domain.string, 1);
            _dst.enc_ndr_referent(domain_sid, 1);
            int _LMSessKey_keys = 8;
            int _LMSessKey_keyi = _dst.index;
            _dst.advance(1 * _LMSessKey_keys);
            _dst.enc_ndr_long(acct_flags);
            int _unknowns = 7;
            int _unknowni = _dst.index;
            _dst.advance(4 * _unknowns);

            if (account_name.string != null) {
                _dst = _dst.deferred;
                int _account_name_stringl = account_name.length/2;
                int _account_name_strings = account_name.size/2;
                _dst.enc_ndr_long(_account_name_strings);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_account_name_stringl);
                int _account_name_stringi = _dst.index;
                _dst.advance(2 * _account_name_stringl);

                _dst = _dst.derive(_account_name_stringi);
                for (int _i = 0; _i < _account_name_stringl; _i++) {
                    _dst.enc_ndr_short(account_name.string[_i]);
                }
            }
            if (full_name.string != null) {
                _dst = _dst.deferred;
                int _full_name_stringl = full_name.length/2;
                int _full_name_strings = full_name.size/2;
                _dst.enc_ndr_long(_full_name_strings);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_full_name_stringl);
                int _full_name_stringi = _dst.index;
                _dst.advance(2 * _full_name_stringl);

                _dst = _dst.derive(_full_name_stringi);
                for (int _i = 0; _i < _full_name_stringl; _i++) {
                    _dst.enc_ndr_short(full_name.string[_i]);
                }
            }
            if (logon_script.string != null) {
                _dst = _dst.deferred;
                int _logon_script_stringl = logon_script.length/2;
                int _logon_script_strings = logon_script.size/2;
                _dst.enc_ndr_long(_logon_script_strings);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_logon_script_stringl);
                int _logon_script_stringi = _dst.index;
                _dst.advance(2 * _logon_script_stringl);

                _dst = _dst.derive(_logon_script_stringi);
                for (int _i = 0; _i < _logon_script_stringl; _i++) {
                    _dst.enc_ndr_short(logon_script.string[_i]);
                }
            }
            if (profile_path.string != null) {
                _dst = _dst.deferred;
                int _profile_path_stringl = profile_path.length/2;
                int _profile_path_strings = profile_path.size/2;
                _dst.enc_ndr_long(_profile_path_strings);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_profile_path_stringl);
                int _profile_path_stringi = _dst.index;
                _dst.advance(2 * _profile_path_stringl);

                _dst = _dst.derive(_profile_path_stringi);
                for (int _i = 0; _i < _profile_path_stringl; _i++) {
                    _dst.enc_ndr_short(profile_path.string[_i]);
                }
            }
            if (home_directory.string != null) {
                _dst = _dst.deferred;
                int _home_directory_stringl = home_directory.length/2;
                int _home_directory_strings = home_directory.size/2;
                _dst.enc_ndr_long(_home_directory_strings);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_home_directory_stringl);
                int _home_directory_stringi = _dst.index;
                _dst.advance(2 * _home_directory_stringl);

                _dst = _dst.derive(_home_directory_stringi);
                for (int _i = 0; _i < _home_directory_stringl; _i++) {
                    _dst.enc_ndr_short(home_directory.string[_i]);
                }
            }
            if (home_drive.string != null) {
                _dst = _dst.deferred;
                int _home_drive_stringl = home_drive.length/2;
                int _home_drive_strings = home_drive.size/2;
                _dst.enc_ndr_long(_home_drive_strings);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_home_drive_stringl);
                int _home_drive_stringi = _dst.index;
                _dst.advance(2 * _home_drive_stringl);

                _dst = _dst.derive(_home_drive_stringi);
                for (int _i = 0; _i < _home_drive_stringl; _i++) {
                    _dst.enc_ndr_short(home_drive.string[_i]);
                }
            }
            if (groups.rids != null) {
                _dst = _dst.deferred;
                int _groups_ridss = groups.count;
                _dst.enc_ndr_long(_groups_ridss);
                int _groups_ridsi = _dst.index;
                _dst.advance(8 * _groups_ridss);

                _dst = _dst.derive(_groups_ridsi);
                for (int _i = 0; _i < _groups_ridss; _i++) {
                    groups.rids[_i].encode(_dst);
                }
            }
            _dst = _dst.derive(_key_keyi);
            for (int _i = 0; _i < _key_keys; _i++) {
                _dst.enc_ndr_small(key.key[_i]);
            }
            if (logon_server.string != null) {
                _dst = _dst.deferred;
                int _logon_server_stringl = logon_server.length/2;
                int _logon_server_strings = logon_server.size/2;
                _dst.enc_ndr_long(_logon_server_strings);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_logon_server_stringl);
                int _logon_server_stringi = _dst.index;
                _dst.advance(2 * _logon_server_stringl);

                _dst = _dst.derive(_logon_server_stringi);
                for (int _i = 0; _i < _logon_server_stringl; _i++) {
                    _dst.enc_ndr_short(logon_server.string[_i]);
                }
            }
            if (domain.string != null) {
                _dst = _dst.deferred;
                int _domain_stringl = domain.length/2;
                int _domain_strings = domain.size/2;
                _dst.enc_ndr_long(_domain_strings);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_domain_stringl);
                int _domain_stringi = _dst.index;
                _dst.advance(2 * _domain_stringl);

                _dst = _dst.derive(_domain_stringi);
                for (int _i = 0; _i < _domain_stringl; _i++) {
                    _dst.enc_ndr_short(domain.string[_i]);
                }
            }
            if (domain_sid != null) {
                _dst = _dst.deferred;
                domain_sid.encode(_dst);

            }
            _dst = _dst.derive(_LMSessKey_keyi);
            for (int _i = 0; _i < _LMSessKey_keys; _i++) {
                _dst.enc_ndr_small(LMSessKey.key[_i]);
            }
            _dst = _dst.derive(_unknowni);
            for (int _i = 0; _i < _unknowns; _i++) {
                _dst.enc_ndr_long(unknown[_i]);
            }
        }
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(8);
            last_logon = (long)_src.dec_ndr_hyper();
            last_logoff = (long)_src.dec_ndr_hyper();
            acct_expiry = (long)_src.dec_ndr_hyper();
            last_password_change = (long)_src.dec_ndr_hyper();
            allow_password_change = (long)_src.dec_ndr_hyper();
            force_password_change = (long)_src.dec_ndr_hyper();
            _src.align(4);
            if (account_name == null) {
                account_name = new lsa_String();
            }
            account_name.length = (short)_src.dec_ndr_short();
            account_name.size = (short)_src.dec_ndr_short();
            int _account_name_stringp = _src.dec_ndr_long();
            _src.align(4);
            if (full_name == null) {
                full_name = new lsa_String();
            }
            full_name.length = (short)_src.dec_ndr_short();
            full_name.size = (short)_src.dec_ndr_short();
            int _full_name_stringp = _src.dec_ndr_long();
            _src.align(4);
            if (logon_script == null) {
                logon_script = new lsa_String();
            }
            logon_script.length = (short)_src.dec_ndr_short();
            logon_script.size = (short)_src.dec_ndr_short();
            int _logon_script_stringp = _src.dec_ndr_long();
            _src.align(4);
            if (profile_path == null) {
                profile_path = new lsa_String();
            }
            profile_path.length = (short)_src.dec_ndr_short();
            profile_path.size = (short)_src.dec_ndr_short();
            int _profile_path_stringp = _src.dec_ndr_long();
            _src.align(4);
            if (home_directory == null) {
                home_directory = new lsa_String();
            }
            home_directory.length = (short)_src.dec_ndr_short();
            home_directory.size = (short)_src.dec_ndr_short();
            int _home_directory_stringp = _src.dec_ndr_long();
            _src.align(4);
            if (home_drive == null) {
                home_drive = new lsa_String();
            }
            home_drive.length = (short)_src.dec_ndr_short();
            home_drive.size = (short)_src.dec_ndr_short();
            int _home_drive_stringp = _src.dec_ndr_long();
            logon_count = (short)_src.dec_ndr_short();
            bad_password_count = (short)_src.dec_ndr_short();
            rid = (int)_src.dec_ndr_long();
            primary_gid = (int)_src.dec_ndr_long();
            _src.align(4);
            if (groups == null) {
                groups = new samr_RidWithAttributeArray();
            }
            groups.count = (int)_src.dec_ndr_long();
            int _groups_ridsp = _src.dec_ndr_long();
            user_flags = (int)_src.dec_ndr_long();
            _src.align(1);
            if (key == null) {
                key = new netr_UserSessionKey();
            }
            int _key_keys = 16;
            int _key_keyi = _src.index;
            _src.advance(1 * _key_keys);
            _src.align(4);
            if (logon_server == null) {
                logon_server = new lsa_StringLarge();
            }
            logon_server.length = (short)_src.dec_ndr_short();
            logon_server.size = (short)_src.dec_ndr_short();
            int _logon_server_stringp = _src.dec_ndr_long();
            _src.align(4);
            if (domain == null) {
                domain = new lsa_StringLarge();
            }
            domain.length = (short)_src.dec_ndr_short();
            domain.size = (short)_src.dec_ndr_short();
            int _domain_stringp = _src.dec_ndr_long();
            int _domain_sidp = _src.dec_ndr_long();
            _src.align(1);
            if (LMSessKey == null) {
                LMSessKey = new netr_LMSessionKey();
            }
            int _LMSessKey_keys = 8;
            int _LMSessKey_keyi = _src.index;
            _src.advance(1 * _LMSessKey_keys);
            acct_flags = (int)_src.dec_ndr_long();
            int _unknowns = 7;
            int _unknowni = _src.index;
            _src.advance(4 * _unknowns);

            if (_account_name_stringp != 0) {
                _src = _src.deferred;
                int _account_name_strings = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _account_name_stringl = _src.dec_ndr_long();
                int _account_name_stringi = _src.index;
                _src.advance(2 * _account_name_stringl);

                if (account_name.string == null) {
                    if (_account_name_strings < 0 || _account_name_strings > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    account_name.string = new short[_account_name_strings];
                }
                _src = _src.derive(_account_name_stringi);
                for (int _i = 0; _i < _account_name_stringl; _i++) {
                    account_name.string[_i] = (short)_src.dec_ndr_short();
                }
            }
            if (_full_name_stringp != 0) {
                _src = _src.deferred;
                int _full_name_strings = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _full_name_stringl = _src.dec_ndr_long();
                int _full_name_stringi = _src.index;
                _src.advance(2 * _full_name_stringl);

                if (full_name.string == null) {
                    if (_full_name_strings < 0 || _full_name_strings > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    full_name.string = new short[_full_name_strings];
                }
                _src = _src.derive(_full_name_stringi);
                for (int _i = 0; _i < _full_name_stringl; _i++) {
                    full_name.string[_i] = (short)_src.dec_ndr_short();
                }
            }
            if (_logon_script_stringp != 0) {
                _src = _src.deferred;
                int _logon_script_strings = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _logon_script_stringl = _src.dec_ndr_long();
                int _logon_script_stringi = _src.index;
                _src.advance(2 * _logon_script_stringl);

                if (logon_script.string == null) {
                    if (_logon_script_strings < 0 || _logon_script_strings > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    logon_script.string = new short[_logon_script_strings];
                }
                _src = _src.derive(_logon_script_stringi);
                for (int _i = 0; _i < _logon_script_stringl; _i++) {
                    logon_script.string[_i] = (short)_src.dec_ndr_short();
                }
            }
            if (_profile_path_stringp != 0) {
                _src = _src.deferred;
                int _profile_path_strings = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _profile_path_stringl = _src.dec_ndr_long();
                int _profile_path_stringi = _src.index;
                _src.advance(2 * _profile_path_stringl);

                if (profile_path.string == null) {
                    if (_profile_path_strings < 0 || _profile_path_strings > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    profile_path.string = new short[_profile_path_strings];
                }
                _src = _src.derive(_profile_path_stringi);
                for (int _i = 0; _i < _profile_path_stringl; _i++) {
                    profile_path.string[_i] = (short)_src.dec_ndr_short();
                }
            }
            if (_home_directory_stringp != 0) {
                _src = _src.deferred;
                int _home_directory_strings = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _home_directory_stringl = _src.dec_ndr_long();
                int _home_directory_stringi = _src.index;
                _src.advance(2 * _home_directory_stringl);

                if (home_directory.string == null) {
                    if (_home_directory_strings < 0 || _home_directory_strings > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    home_directory.string = new short[_home_directory_strings];
                }
                _src = _src.derive(_home_directory_stringi);
                for (int _i = 0; _i < _home_directory_stringl; _i++) {
                    home_directory.string[_i] = (short)_src.dec_ndr_short();
                }
            }
            if (_home_drive_stringp != 0) {
                _src = _src.deferred;
                int _home_drive_strings = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _home_drive_stringl = _src.dec_ndr_long();
                int _home_drive_stringi = _src.index;
                _src.advance(2 * _home_drive_stringl);

                if (home_drive.string == null) {
                    if (_home_drive_strings < 0 || _home_drive_strings > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    home_drive.string = new short[_home_drive_strings];
                }
                _src = _src.derive(_home_drive_stringi);
                for (int _i = 0; _i < _home_drive_stringl; _i++) {
                    home_drive.string[_i] = (short)_src.dec_ndr_short();
                }
            }
            if (_groups_ridsp != 0) {
                _src = _src.deferred;
                int _groups_ridss = _src.dec_ndr_long();
                int _groups_ridsi = _src.index;
                _src.advance(8 * _groups_ridss);

                if (groups.rids == null) {
                    if (_groups_ridss < 0 || _groups_ridss > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    groups.rids = new samr_RidWithAttribute[_groups_ridss];
                }
                _src = _src.derive(_groups_ridsi);
                for (int _i = 0; _i < _groups_ridss; _i++) {
                    if (groups.rids[_i] == null) {
                        groups.rids[_i] = new samr_RidWithAttribute();
                    }
                    groups.rids[_i].decode(_src);
                }
            }
            if (key.key == null) {
                if (_key_keys < 0 || _key_keys > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                key.key = new byte[_key_keys];
            }
            _src = _src.derive(_key_keyi);
            for (int _i = 0; _i < _key_keys; _i++) {
                key.key[_i] = (byte)_src.dec_ndr_small();
            }
            if (_logon_server_stringp != 0) {
                _src = _src.deferred;
                int _logon_server_strings = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _logon_server_stringl = _src.dec_ndr_long();
                int _logon_server_stringi = _src.index;
                _src.advance(2 * _logon_server_stringl);

                if (logon_server.string == null) {
                    if (_logon_server_strings < 0 || _logon_server_strings > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    logon_server.string = new short[_logon_server_strings];
                }
                _src = _src.derive(_logon_server_stringi);
                for (int _i = 0; _i < _logon_server_stringl; _i++) {
                    logon_server.string[_i] = (short)_src.dec_ndr_short();
                }
            }
            if (_domain_stringp != 0) {
                _src = _src.deferred;
                int _domain_strings = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _domain_stringl = _src.dec_ndr_long();
                int _domain_stringi = _src.index;
                _src.advance(2 * _domain_stringl);

                if (domain.string == null) {
                    if (_domain_strings < 0 || _domain_strings > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    domain.string = new short[_domain_strings];
                }
                _src = _src.derive(_domain_stringi);
                for (int _i = 0; _i < _domain_stringl; _i++) {
                    domain.string[_i] = (short)_src.dec_ndr_short();
                }
            }
            if (_domain_sidp != 0) {
                if (domain_sid == null) { /* YOYOYO */
                    domain_sid = new dom_sid();
                }
                _src = _src.deferred;
                domain_sid.decode(_src);

            }
            if (LMSessKey.key == null) {
                if (_LMSessKey_keys < 0 || _LMSessKey_keys > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                LMSessKey.key = new byte[_LMSessKey_keys];
            }
            _src = _src.derive(_LMSessKey_keyi);
            for (int _i = 0; _i < _LMSessKey_keys; _i++) {
                LMSessKey.key[_i] = (byte)_src.dec_ndr_small();
            }
            if (unknown == null) {
                if (_unknowns < 0 || _unknowns > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                unknown = new int[_unknowns];
            }
            _src = _src.derive(_unknowni);
            for (int _i = 0; _i < _unknowns; _i++) {
                unknown[_i] = (int)_src.dec_ndr_long();
            }
        }
    }
    public static class netr_SamInfo2 extends NdrObject {

        public netr_SamBaseInfo base;

        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(8);
            _dst.enc_ndr_hyper(base.last_logon);
            _dst.enc_ndr_hyper(base.last_logoff);
            _dst.enc_ndr_hyper(base.acct_expiry);
            _dst.enc_ndr_hyper(base.last_password_change);
            _dst.enc_ndr_hyper(base.allow_password_change);
            _dst.enc_ndr_hyper(base.force_password_change);
            _dst.enc_ndr_short(base.account_name.length);
            _dst.enc_ndr_short(base.account_name.size);
            _dst.enc_ndr_referent(base.account_name.string, 1);
            _dst.enc_ndr_short(base.full_name.length);
            _dst.enc_ndr_short(base.full_name.size);
            _dst.enc_ndr_referent(base.full_name.string, 1);
            _dst.enc_ndr_short(base.logon_script.length);
            _dst.enc_ndr_short(base.logon_script.size);
            _dst.enc_ndr_referent(base.logon_script.string, 1);
            _dst.enc_ndr_short(base.profile_path.length);
            _dst.enc_ndr_short(base.profile_path.size);
            _dst.enc_ndr_referent(base.profile_path.string, 1);
            _dst.enc_ndr_short(base.home_directory.length);
            _dst.enc_ndr_short(base.home_directory.size);
            _dst.enc_ndr_referent(base.home_directory.string, 1);
            _dst.enc_ndr_short(base.home_drive.length);
            _dst.enc_ndr_short(base.home_drive.size);
            _dst.enc_ndr_referent(base.home_drive.string, 1);
            _dst.enc_ndr_short(base.logon_count);
            _dst.enc_ndr_short(base.bad_password_count);
            _dst.enc_ndr_long(base.rid);
            _dst.enc_ndr_long(base.primary_gid);
            _dst.enc_ndr_long(base.groups.count);
            _dst.enc_ndr_referent(base.groups.rids, 1);
            _dst.enc_ndr_long(base.user_flags);
            int _base_key_keys = 16;
            int _base_key_keyi = _dst.index;
            _dst.advance(1 * _base_key_keys);
            _dst.enc_ndr_short(base.logon_server.length);
            _dst.enc_ndr_short(base.logon_server.size);
            _dst.enc_ndr_referent(base.logon_server.string, 1);
            _dst.enc_ndr_short(base.domain.length);
            _dst.enc_ndr_short(base.domain.size);
            _dst.enc_ndr_referent(base.domain.string, 1);
            _dst.enc_ndr_referent(base.domain_sid, 1);
            int _base_LMSessKey_keys = 8;
            int _base_LMSessKey_keyi = _dst.index;
            _dst.advance(1 * _base_LMSessKey_keys);
            _dst.enc_ndr_long(base.acct_flags);
            int _base_unknowns = 7;
            int _base_unknowni = _dst.index;
            _dst.advance(4 * _base_unknowns);

            if (base.account_name.string != null) {
                _dst = _dst.deferred;
                int _base_account_name_stringl = base.account_name.length/2;
                int _base_account_name_strings = base.account_name.size/2;
                _dst.enc_ndr_long(_base_account_name_strings);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_base_account_name_stringl);
                int _base_account_name_stringi = _dst.index;
                _dst.advance(2 * _base_account_name_stringl);

                _dst = _dst.derive(_base_account_name_stringi);
                for (int _i = 0; _i < _base_account_name_stringl; _i++) {
                    _dst.enc_ndr_short(base.account_name.string[_i]);
                }
            }
            if (base.full_name.string != null) {
                _dst = _dst.deferred;
                int _base_full_name_stringl = base.full_name.length/2;
                int _base_full_name_strings = base.full_name.size/2;
                _dst.enc_ndr_long(_base_full_name_strings);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_base_full_name_stringl);
                int _base_full_name_stringi = _dst.index;
                _dst.advance(2 * _base_full_name_stringl);

                _dst = _dst.derive(_base_full_name_stringi);
                for (int _i = 0; _i < _base_full_name_stringl; _i++) {
                    _dst.enc_ndr_short(base.full_name.string[_i]);
                }
            }
            if (base.logon_script.string != null) {
                _dst = _dst.deferred;
                int _base_logon_script_stringl = base.logon_script.length/2;
                int _base_logon_script_strings = base.logon_script.size/2;
                _dst.enc_ndr_long(_base_logon_script_strings);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_base_logon_script_stringl);
                int _base_logon_script_stringi = _dst.index;
                _dst.advance(2 * _base_logon_script_stringl);

                _dst = _dst.derive(_base_logon_script_stringi);
                for (int _i = 0; _i < _base_logon_script_stringl; _i++) {
                    _dst.enc_ndr_short(base.logon_script.string[_i]);
                }
            }
            if (base.profile_path.string != null) {
                _dst = _dst.deferred;
                int _base_profile_path_stringl = base.profile_path.length/2;
                int _base_profile_path_strings = base.profile_path.size/2;
                _dst.enc_ndr_long(_base_profile_path_strings);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_base_profile_path_stringl);
                int _base_profile_path_stringi = _dst.index;
                _dst.advance(2 * _base_profile_path_stringl);

                _dst = _dst.derive(_base_profile_path_stringi);
                for (int _i = 0; _i < _base_profile_path_stringl; _i++) {
                    _dst.enc_ndr_short(base.profile_path.string[_i]);
                }
            }
            if (base.home_directory.string != null) {
                _dst = _dst.deferred;
                int _base_home_directory_stringl = base.home_directory.length/2;
                int _base_home_directory_strings = base.home_directory.size/2;
                _dst.enc_ndr_long(_base_home_directory_strings);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_base_home_directory_stringl);
                int _base_home_directory_stringi = _dst.index;
                _dst.advance(2 * _base_home_directory_stringl);

                _dst = _dst.derive(_base_home_directory_stringi);
                for (int _i = 0; _i < _base_home_directory_stringl; _i++) {
                    _dst.enc_ndr_short(base.home_directory.string[_i]);
                }
            }
            if (base.home_drive.string != null) {
                _dst = _dst.deferred;
                int _base_home_drive_stringl = base.home_drive.length/2;
                int _base_home_drive_strings = base.home_drive.size/2;
                _dst.enc_ndr_long(_base_home_drive_strings);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_base_home_drive_stringl);
                int _base_home_drive_stringi = _dst.index;
                _dst.advance(2 * _base_home_drive_stringl);

                _dst = _dst.derive(_base_home_drive_stringi);
                for (int _i = 0; _i < _base_home_drive_stringl; _i++) {
                    _dst.enc_ndr_short(base.home_drive.string[_i]);
                }
            }
            if (base.groups.rids != null) {
                _dst = _dst.deferred;
                int _base_groups_ridss = base.groups.count;
                _dst.enc_ndr_long(_base_groups_ridss);
                int _base_groups_ridsi = _dst.index;
                _dst.advance(8 * _base_groups_ridss);

                _dst = _dst.derive(_base_groups_ridsi);
                for (int _i = 0; _i < _base_groups_ridss; _i++) {
                    base.groups.rids[_i].encode(_dst);
                }
            }
            _dst = _dst.derive(_base_key_keyi);
            for (int _i = 0; _i < _base_key_keys; _i++) {
                _dst.enc_ndr_small(base.key.key[_i]);
            }
            if (base.logon_server.string != null) {
                _dst = _dst.deferred;
                int _base_logon_server_stringl = base.logon_server.length/2;
                int _base_logon_server_strings = base.logon_server.size/2;
                _dst.enc_ndr_long(_base_logon_server_strings);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_base_logon_server_stringl);
                int _base_logon_server_stringi = _dst.index;
                _dst.advance(2 * _base_logon_server_stringl);

                _dst = _dst.derive(_base_logon_server_stringi);
                for (int _i = 0; _i < _base_logon_server_stringl; _i++) {
                    _dst.enc_ndr_short(base.logon_server.string[_i]);
                }
            }
            if (base.domain.string != null) {
                _dst = _dst.deferred;
                int _base_domain_stringl = base.domain.length/2;
                int _base_domain_strings = base.domain.size/2;
                _dst.enc_ndr_long(_base_domain_strings);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_base_domain_stringl);
                int _base_domain_stringi = _dst.index;
                _dst.advance(2 * _base_domain_stringl);

                _dst = _dst.derive(_base_domain_stringi);
                for (int _i = 0; _i < _base_domain_stringl; _i++) {
                    _dst.enc_ndr_short(base.domain.string[_i]);
                }
            }
            if (base.domain_sid != null) {
                _dst = _dst.deferred;
                base.domain_sid.encode(_dst);

            }
            _dst = _dst.derive(_base_LMSessKey_keyi);
            for (int _i = 0; _i < _base_LMSessKey_keys; _i++) {
                _dst.enc_ndr_small(base.LMSessKey.key[_i]);
            }
            _dst = _dst.derive(_base_unknowni);
            for (int _i = 0; _i < _base_unknowns; _i++) {
                _dst.enc_ndr_long(base.unknown[_i]);
            }
        }
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(8);
            _src.align(8);
            if (base == null) {
                base = new netr_SamBaseInfo();
            }
            base.last_logon = (long)_src.dec_ndr_hyper();
            base.last_logoff = (long)_src.dec_ndr_hyper();
            base.acct_expiry = (long)_src.dec_ndr_hyper();
            base.last_password_change = (long)_src.dec_ndr_hyper();
            base.allow_password_change = (long)_src.dec_ndr_hyper();
            base.force_password_change = (long)_src.dec_ndr_hyper();
            _src.align(4);
            if (base.account_name == null) {
                base.account_name = new lsa_String();
            }
            base.account_name.length = (short)_src.dec_ndr_short();
            base.account_name.size = (short)_src.dec_ndr_short();
            int _base_account_name_stringp = _src.dec_ndr_long();
            _src.align(4);
            if (base.full_name == null) {
                base.full_name = new lsa_String();
            }
            base.full_name.length = (short)_src.dec_ndr_short();
            base.full_name.size = (short)_src.dec_ndr_short();
            int _base_full_name_stringp = _src.dec_ndr_long();
            _src.align(4);
            if (base.logon_script == null) {
                base.logon_script = new lsa_String();
            }
            base.logon_script.length = (short)_src.dec_ndr_short();
            base.logon_script.size = (short)_src.dec_ndr_short();
            int _base_logon_script_stringp = _src.dec_ndr_long();
            _src.align(4);
            if (base.profile_path == null) {
                base.profile_path = new lsa_String();
            }
            base.profile_path.length = (short)_src.dec_ndr_short();
            base.profile_path.size = (short)_src.dec_ndr_short();
            int _base_profile_path_stringp = _src.dec_ndr_long();
            _src.align(4);
            if (base.home_directory == null) {
                base.home_directory = new lsa_String();
            }
            base.home_directory.length = (short)_src.dec_ndr_short();
            base.home_directory.size = (short)_src.dec_ndr_short();
            int _base_home_directory_stringp = _src.dec_ndr_long();
            _src.align(4);
            if (base.home_drive == null) {
                base.home_drive = new lsa_String();
            }
            base.home_drive.length = (short)_src.dec_ndr_short();
            base.home_drive.size = (short)_src.dec_ndr_short();
            int _base_home_drive_stringp = _src.dec_ndr_long();
            base.logon_count = (short)_src.dec_ndr_short();
            base.bad_password_count = (short)_src.dec_ndr_short();
            base.rid = (int)_src.dec_ndr_long();
            base.primary_gid = (int)_src.dec_ndr_long();
            _src.align(4);
            if (base.groups == null) {
                base.groups = new samr_RidWithAttributeArray();
            }
            base.groups.count = (int)_src.dec_ndr_long();
            int _base_groups_ridsp = _src.dec_ndr_long();
            base.user_flags = (int)_src.dec_ndr_long();
            _src.align(1);
            if (base.key == null) {
                base.key = new netr_UserSessionKey();
            }
            int _base_key_keys = 16;
            int _base_key_keyi = _src.index;
            _src.advance(1 * _base_key_keys);
            _src.align(4);
            if (base.logon_server == null) {
                base.logon_server = new lsa_StringLarge();
            }
            base.logon_server.length = (short)_src.dec_ndr_short();
            base.logon_server.size = (short)_src.dec_ndr_short();
            int _base_logon_server_stringp = _src.dec_ndr_long();
            _src.align(4);
            if (base.domain == null) {
                base.domain = new lsa_StringLarge();
            }
            base.domain.length = (short)_src.dec_ndr_short();
            base.domain.size = (short)_src.dec_ndr_short();
            int _base_domain_stringp = _src.dec_ndr_long();
            int _base_domain_sidp = _src.dec_ndr_long();
            _src.align(1);
            if (base.LMSessKey == null) {
                base.LMSessKey = new netr_LMSessionKey();
            }
            int _base_LMSessKey_keys = 8;
            int _base_LMSessKey_keyi = _src.index;
            _src.advance(1 * _base_LMSessKey_keys);
            base.acct_flags = (int)_src.dec_ndr_long();
            int _base_unknowns = 7;
            int _base_unknowni = _src.index;
            _src.advance(4 * _base_unknowns);

            if (_base_account_name_stringp != 0) {
                _src = _src.deferred;
                int _base_account_name_strings = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _base_account_name_stringl = _src.dec_ndr_long();
                int _base_account_name_stringi = _src.index;
                _src.advance(2 * _base_account_name_stringl);

                if (base.account_name.string == null) {
                    if (_base_account_name_strings < 0 || _base_account_name_strings > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    base.account_name.string = new short[_base_account_name_strings];
                }
                _src = _src.derive(_base_account_name_stringi);
                for (int _i = 0; _i < _base_account_name_stringl; _i++) {
                    base.account_name.string[_i] = (short)_src.dec_ndr_short();
                }
            }
            if (_base_full_name_stringp != 0) {
                _src = _src.deferred;
                int _base_full_name_strings = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _base_full_name_stringl = _src.dec_ndr_long();
                int _base_full_name_stringi = _src.index;
                _src.advance(2 * _base_full_name_stringl);

                if (base.full_name.string == null) {
                    if (_base_full_name_strings < 0 || _base_full_name_strings > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    base.full_name.string = new short[_base_full_name_strings];
                }
                _src = _src.derive(_base_full_name_stringi);
                for (int _i = 0; _i < _base_full_name_stringl; _i++) {
                    base.full_name.string[_i] = (short)_src.dec_ndr_short();
                }
            }
            if (_base_logon_script_stringp != 0) {
                _src = _src.deferred;
                int _base_logon_script_strings = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _base_logon_script_stringl = _src.dec_ndr_long();
                int _base_logon_script_stringi = _src.index;
                _src.advance(2 * _base_logon_script_stringl);

                if (base.logon_script.string == null) {
                    if (_base_logon_script_strings < 0 || _base_logon_script_strings > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    base.logon_script.string = new short[_base_logon_script_strings];
                }
                _src = _src.derive(_base_logon_script_stringi);
                for (int _i = 0; _i < _base_logon_script_stringl; _i++) {
                    base.logon_script.string[_i] = (short)_src.dec_ndr_short();
                }
            }
            if (_base_profile_path_stringp != 0) {
                _src = _src.deferred;
                int _base_profile_path_strings = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _base_profile_path_stringl = _src.dec_ndr_long();
                int _base_profile_path_stringi = _src.index;
                _src.advance(2 * _base_profile_path_stringl);

                if (base.profile_path.string == null) {
                    if (_base_profile_path_strings < 0 || _base_profile_path_strings > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    base.profile_path.string = new short[_base_profile_path_strings];
                }
                _src = _src.derive(_base_profile_path_stringi);
                for (int _i = 0; _i < _base_profile_path_stringl; _i++) {
                    base.profile_path.string[_i] = (short)_src.dec_ndr_short();
                }
            }
            if (_base_home_directory_stringp != 0) {
                _src = _src.deferred;
                int _base_home_directory_strings = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _base_home_directory_stringl = _src.dec_ndr_long();
                int _base_home_directory_stringi = _src.index;
                _src.advance(2 * _base_home_directory_stringl);

                if (base.home_directory.string == null) {
                    if (_base_home_directory_strings < 0 || _base_home_directory_strings > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    base.home_directory.string = new short[_base_home_directory_strings];
                }
                _src = _src.derive(_base_home_directory_stringi);
                for (int _i = 0; _i < _base_home_directory_stringl; _i++) {
                    base.home_directory.string[_i] = (short)_src.dec_ndr_short();
                }
            }
            if (_base_home_drive_stringp != 0) {
                _src = _src.deferred;
                int _base_home_drive_strings = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _base_home_drive_stringl = _src.dec_ndr_long();
                int _base_home_drive_stringi = _src.index;
                _src.advance(2 * _base_home_drive_stringl);

                if (base.home_drive.string == null) {
                    if (_base_home_drive_strings < 0 || _base_home_drive_strings > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    base.home_drive.string = new short[_base_home_drive_strings];
                }
                _src = _src.derive(_base_home_drive_stringi);
                for (int _i = 0; _i < _base_home_drive_stringl; _i++) {
                    base.home_drive.string[_i] = (short)_src.dec_ndr_short();
                }
            }
            if (_base_groups_ridsp != 0) {
                _src = _src.deferred;
                int _base_groups_ridss = _src.dec_ndr_long();
                int _base_groups_ridsi = _src.index;
                _src.advance(8 * _base_groups_ridss);

                if (base.groups.rids == null) {
                    if (_base_groups_ridss < 0 || _base_groups_ridss > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    base.groups.rids = new samr_RidWithAttribute[_base_groups_ridss];
                }
                _src = _src.derive(_base_groups_ridsi);
                for (int _i = 0; _i < _base_groups_ridss; _i++) {
                    if (base.groups.rids[_i] == null) {
                        base.groups.rids[_i] = new samr_RidWithAttribute();
                    }
                    base.groups.rids[_i].decode(_src);
                }
            }
            if (base.key.key == null) {
                if (_base_key_keys < 0 || _base_key_keys > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                base.key.key = new byte[_base_key_keys];
            }
            _src = _src.derive(_base_key_keyi);
            for (int _i = 0; _i < _base_key_keys; _i++) {
                base.key.key[_i] = (byte)_src.dec_ndr_small();
            }
            if (_base_logon_server_stringp != 0) {
                _src = _src.deferred;
                int _base_logon_server_strings = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _base_logon_server_stringl = _src.dec_ndr_long();
                int _base_logon_server_stringi = _src.index;
                _src.advance(2 * _base_logon_server_stringl);

                if (base.logon_server.string == null) {
                    if (_base_logon_server_strings < 0 || _base_logon_server_strings > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    base.logon_server.string = new short[_base_logon_server_strings];
                }
                _src = _src.derive(_base_logon_server_stringi);
                for (int _i = 0; _i < _base_logon_server_stringl; _i++) {
                    base.logon_server.string[_i] = (short)_src.dec_ndr_short();
                }
            }
            if (_base_domain_stringp != 0) {
                _src = _src.deferred;
                int _base_domain_strings = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _base_domain_stringl = _src.dec_ndr_long();
                int _base_domain_stringi = _src.index;
                _src.advance(2 * _base_domain_stringl);

                if (base.domain.string == null) {
                    if (_base_domain_strings < 0 || _base_domain_strings > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    base.domain.string = new short[_base_domain_strings];
                }
                _src = _src.derive(_base_domain_stringi);
                for (int _i = 0; _i < _base_domain_stringl; _i++) {
                    base.domain.string[_i] = (short)_src.dec_ndr_short();
                }
            }
            if (_base_domain_sidp != 0) {
                if (base.domain_sid == null) { /* YOYOYO */
                    base.domain_sid = new dom_sid();
                }
                _src = _src.deferred;
                base.domain_sid.decode(_src);

            }
            if (base.LMSessKey.key == null) {
                if (_base_LMSessKey_keys < 0 || _base_LMSessKey_keys > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                base.LMSessKey.key = new byte[_base_LMSessKey_keys];
            }
            _src = _src.derive(_base_LMSessKey_keyi);
            for (int _i = 0; _i < _base_LMSessKey_keys; _i++) {
                base.LMSessKey.key[_i] = (byte)_src.dec_ndr_small();
            }
            if (base.unknown == null) {
                if (_base_unknowns < 0 || _base_unknowns > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                base.unknown = new int[_base_unknowns];
            }
            _src = _src.derive(_base_unknowni);
            for (int _i = 0; _i < _base_unknowns; _i++) {
                base.unknown[_i] = (int)_src.dec_ndr_long();
            }
        }
    }
    public static class netr_SidAttr extends NdrObject {

        public dom_sid sid;
        public int attributes;

        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_referent(sid, 1);
            _dst.enc_ndr_long(attributes);

            if (sid != null) {
                _dst = _dst.deferred;
                sid.encode(_dst);

            }
        }
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            int _sidp = _src.dec_ndr_long();
            attributes = (int)_src.dec_ndr_long();

            if (_sidp != 0) {
                if (sid == null) { /* YOYOYO */
                    sid = new dom_sid();
                }
                _src = _src.deferred;
                sid.decode(_src);

            }
        }
    }
    public static class netr_SamInfo6 extends NdrObject {

        public netr_SamBaseInfo base;
        public int sidcount;
        public netr_SidAttr[] sids;
        public lsa_String forest;
        public lsa_String principle;
        public int[] unknown4;

        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(8);
            _dst.enc_ndr_hyper(base.last_logon);
            _dst.enc_ndr_hyper(base.last_logoff);
            _dst.enc_ndr_hyper(base.acct_expiry);
            _dst.enc_ndr_hyper(base.last_password_change);
            _dst.enc_ndr_hyper(base.allow_password_change);
            _dst.enc_ndr_hyper(base.force_password_change);
            _dst.enc_ndr_short(base.account_name.length);
            _dst.enc_ndr_short(base.account_name.size);
            _dst.enc_ndr_referent(base.account_name.string, 1);
            _dst.enc_ndr_short(base.full_name.length);
            _dst.enc_ndr_short(base.full_name.size);
            _dst.enc_ndr_referent(base.full_name.string, 1);
            _dst.enc_ndr_short(base.logon_script.length);
            _dst.enc_ndr_short(base.logon_script.size);
            _dst.enc_ndr_referent(base.logon_script.string, 1);
            _dst.enc_ndr_short(base.profile_path.length);
            _dst.enc_ndr_short(base.profile_path.size);
            _dst.enc_ndr_referent(base.profile_path.string, 1);
            _dst.enc_ndr_short(base.home_directory.length);
            _dst.enc_ndr_short(base.home_directory.size);
            _dst.enc_ndr_referent(base.home_directory.string, 1);
            _dst.enc_ndr_short(base.home_drive.length);
            _dst.enc_ndr_short(base.home_drive.size);
            _dst.enc_ndr_referent(base.home_drive.string, 1);
            _dst.enc_ndr_short(base.logon_count);
            _dst.enc_ndr_short(base.bad_password_count);
            _dst.enc_ndr_long(base.rid);
            _dst.enc_ndr_long(base.primary_gid);
            _dst.enc_ndr_long(base.groups.count);
            _dst.enc_ndr_referent(base.groups.rids, 1);
            _dst.enc_ndr_long(base.user_flags);
            int _base_key_keys = 16;
            int _base_key_keyi = _dst.index;
            _dst.advance(1 * _base_key_keys);
            _dst.enc_ndr_short(base.logon_server.length);
            _dst.enc_ndr_short(base.logon_server.size);
            _dst.enc_ndr_referent(base.logon_server.string, 1);
            _dst.enc_ndr_short(base.domain.length);
            _dst.enc_ndr_short(base.domain.size);
            _dst.enc_ndr_referent(base.domain.string, 1);
            _dst.enc_ndr_referent(base.domain_sid, 1);
            int _base_LMSessKey_keys = 8;
            int _base_LMSessKey_keyi = _dst.index;
            _dst.advance(1 * _base_LMSessKey_keys);
            _dst.enc_ndr_long(base.acct_flags);
            int _base_unknowns = 7;
            int _base_unknowni = _dst.index;
            _dst.advance(4 * _base_unknowns);
            _dst.enc_ndr_long(sidcount);
            _dst.enc_ndr_referent(sids, 1);
            _dst.enc_ndr_short(forest.length);
            _dst.enc_ndr_short(forest.size);
            _dst.enc_ndr_referent(forest.string, 1);
            _dst.enc_ndr_short(principle.length);
            _dst.enc_ndr_short(principle.size);
            _dst.enc_ndr_referent(principle.string, 1);
            int _unknown4s = 20;
            int _unknown4i = _dst.index;
            _dst.advance(4 * _unknown4s);

            if (base.account_name.string != null) {
                _dst = _dst.deferred;
                int _base_account_name_stringl = base.account_name.length/2;
                int _base_account_name_strings = base.account_name.size/2;
                _dst.enc_ndr_long(_base_account_name_strings);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_base_account_name_stringl);
                int _base_account_name_stringi = _dst.index;
                _dst.advance(2 * _base_account_name_stringl);

                _dst = _dst.derive(_base_account_name_stringi);
                for (int _i = 0; _i < _base_account_name_stringl; _i++) {
                    _dst.enc_ndr_short(base.account_name.string[_i]);
                }
            }
            if (base.full_name.string != null) {
                _dst = _dst.deferred;
                int _base_full_name_stringl = base.full_name.length/2;
                int _base_full_name_strings = base.full_name.size/2;
                _dst.enc_ndr_long(_base_full_name_strings);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_base_full_name_stringl);
                int _base_full_name_stringi = _dst.index;
                _dst.advance(2 * _base_full_name_stringl);

                _dst = _dst.derive(_base_full_name_stringi);
                for (int _i = 0; _i < _base_full_name_stringl; _i++) {
                    _dst.enc_ndr_short(base.full_name.string[_i]);
                }
            }
            if (base.logon_script.string != null) {
                _dst = _dst.deferred;
                int _base_logon_script_stringl = base.logon_script.length/2;
                int _base_logon_script_strings = base.logon_script.size/2;
                _dst.enc_ndr_long(_base_logon_script_strings);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_base_logon_script_stringl);
                int _base_logon_script_stringi = _dst.index;
                _dst.advance(2 * _base_logon_script_stringl);

                _dst = _dst.derive(_base_logon_script_stringi);
                for (int _i = 0; _i < _base_logon_script_stringl; _i++) {
                    _dst.enc_ndr_short(base.logon_script.string[_i]);
                }
            }
            if (base.profile_path.string != null) {
                _dst = _dst.deferred;
                int _base_profile_path_stringl = base.profile_path.length/2;
                int _base_profile_path_strings = base.profile_path.size/2;
                _dst.enc_ndr_long(_base_profile_path_strings);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_base_profile_path_stringl);
                int _base_profile_path_stringi = _dst.index;
                _dst.advance(2 * _base_profile_path_stringl);

                _dst = _dst.derive(_base_profile_path_stringi);
                for (int _i = 0; _i < _base_profile_path_stringl; _i++) {
                    _dst.enc_ndr_short(base.profile_path.string[_i]);
                }
            }
            if (base.home_directory.string != null) {
                _dst = _dst.deferred;
                int _base_home_directory_stringl = base.home_directory.length/2;
                int _base_home_directory_strings = base.home_directory.size/2;
                _dst.enc_ndr_long(_base_home_directory_strings);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_base_home_directory_stringl);
                int _base_home_directory_stringi = _dst.index;
                _dst.advance(2 * _base_home_directory_stringl);

                _dst = _dst.derive(_base_home_directory_stringi);
                for (int _i = 0; _i < _base_home_directory_stringl; _i++) {
                    _dst.enc_ndr_short(base.home_directory.string[_i]);
                }
            }
            if (base.home_drive.string != null) {
                _dst = _dst.deferred;
                int _base_home_drive_stringl = base.home_drive.length/2;
                int _base_home_drive_strings = base.home_drive.size/2;
                _dst.enc_ndr_long(_base_home_drive_strings);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_base_home_drive_stringl);
                int _base_home_drive_stringi = _dst.index;
                _dst.advance(2 * _base_home_drive_stringl);

                _dst = _dst.derive(_base_home_drive_stringi);
                for (int _i = 0; _i < _base_home_drive_stringl; _i++) {
                    _dst.enc_ndr_short(base.home_drive.string[_i]);
                }
            }
            if (base.groups.rids != null) {
                _dst = _dst.deferred;
                int _base_groups_ridss = base.groups.count;
                _dst.enc_ndr_long(_base_groups_ridss);
                int _base_groups_ridsi = _dst.index;
                _dst.advance(8 * _base_groups_ridss);

                _dst = _dst.derive(_base_groups_ridsi);
                for (int _i = 0; _i < _base_groups_ridss; _i++) {
                    base.groups.rids[_i].encode(_dst);
                }
            }
            _dst = _dst.derive(_base_key_keyi);
            for (int _i = 0; _i < _base_key_keys; _i++) {
                _dst.enc_ndr_small(base.key.key[_i]);
            }
            if (base.logon_server.string != null) {
                _dst = _dst.deferred;
                int _base_logon_server_stringl = base.logon_server.length/2;
                int _base_logon_server_strings = base.logon_server.size/2;
                _dst.enc_ndr_long(_base_logon_server_strings);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_base_logon_server_stringl);
                int _base_logon_server_stringi = _dst.index;
                _dst.advance(2 * _base_logon_server_stringl);

                _dst = _dst.derive(_base_logon_server_stringi);
                for (int _i = 0; _i < _base_logon_server_stringl; _i++) {
                    _dst.enc_ndr_short(base.logon_server.string[_i]);
                }
            }
            if (base.domain.string != null) {
                _dst = _dst.deferred;
                int _base_domain_stringl = base.domain.length/2;
                int _base_domain_strings = base.domain.size/2;
                _dst.enc_ndr_long(_base_domain_strings);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_base_domain_stringl);
                int _base_domain_stringi = _dst.index;
                _dst.advance(2 * _base_domain_stringl);

                _dst = _dst.derive(_base_domain_stringi);
                for (int _i = 0; _i < _base_domain_stringl; _i++) {
                    _dst.enc_ndr_short(base.domain.string[_i]);
                }
            }
            if (base.domain_sid != null) {
                _dst = _dst.deferred;
                base.domain_sid.encode(_dst);

            }
            _dst = _dst.derive(_base_LMSessKey_keyi);
            for (int _i = 0; _i < _base_LMSessKey_keys; _i++) {
                _dst.enc_ndr_small(base.LMSessKey.key[_i]);
            }
            _dst = _dst.derive(_base_unknowni);
            for (int _i = 0; _i < _base_unknowns; _i++) {
                _dst.enc_ndr_long(base.unknown[_i]);
            }
            if (sids != null) {
                _dst = _dst.deferred;
                int _sidss = sidcount;
                _dst.enc_ndr_long(_sidss);
                int _sidsi = _dst.index;
                _dst.advance(8 * _sidss);

                _dst = _dst.derive(_sidsi);
                for (int _i = 0; _i < _sidss; _i++) {
                    sids[_i].encode(_dst);
                }
            }
            if (forest.string != null) {
                _dst = _dst.deferred;
                int _forest_stringl = forest.length/2;
                int _forest_strings = forest.size/2;
                _dst.enc_ndr_long(_forest_strings);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_forest_stringl);
                int _forest_stringi = _dst.index;
                _dst.advance(2 * _forest_stringl);

                _dst = _dst.derive(_forest_stringi);
                for (int _i = 0; _i < _forest_stringl; _i++) {
                    _dst.enc_ndr_short(forest.string[_i]);
                }
            }
            if (principle.string != null) {
                _dst = _dst.deferred;
                int _principle_stringl = principle.length/2;
                int _principle_strings = principle.size/2;
                _dst.enc_ndr_long(_principle_strings);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_principle_stringl);
                int _principle_stringi = _dst.index;
                _dst.advance(2 * _principle_stringl);

                _dst = _dst.derive(_principle_stringi);
                for (int _i = 0; _i < _principle_stringl; _i++) {
                    _dst.enc_ndr_short(principle.string[_i]);
                }
            }
            _dst = _dst.derive(_unknown4i);
            for (int _i = 0; _i < _unknown4s; _i++) {
                _dst.enc_ndr_long(unknown4[_i]);
            }
        }
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(8);
            _src.align(8);
            if (base == null) {
                base = new netr_SamBaseInfo();
            }
            base.last_logon = (long)_src.dec_ndr_hyper();
            base.last_logoff = (long)_src.dec_ndr_hyper();
            base.acct_expiry = (long)_src.dec_ndr_hyper();
            base.last_password_change = (long)_src.dec_ndr_hyper();
            base.allow_password_change = (long)_src.dec_ndr_hyper();
            base.force_password_change = (long)_src.dec_ndr_hyper();
            _src.align(4);
            if (base.account_name == null) {
                base.account_name = new lsa_String();
            }
            base.account_name.length = (short)_src.dec_ndr_short();
            base.account_name.size = (short)_src.dec_ndr_short();
            int _base_account_name_stringp = _src.dec_ndr_long();
            _src.align(4);
            if (base.full_name == null) {
                base.full_name = new lsa_String();
            }
            base.full_name.length = (short)_src.dec_ndr_short();
            base.full_name.size = (short)_src.dec_ndr_short();
            int _base_full_name_stringp = _src.dec_ndr_long();
            _src.align(4);
            if (base.logon_script == null) {
                base.logon_script = new lsa_String();
            }
            base.logon_script.length = (short)_src.dec_ndr_short();
            base.logon_script.size = (short)_src.dec_ndr_short();
            int _base_logon_script_stringp = _src.dec_ndr_long();
            _src.align(4);
            if (base.profile_path == null) {
                base.profile_path = new lsa_String();
            }
            base.profile_path.length = (short)_src.dec_ndr_short();
            base.profile_path.size = (short)_src.dec_ndr_short();
            int _base_profile_path_stringp = _src.dec_ndr_long();
            _src.align(4);
            if (base.home_directory == null) {
                base.home_directory = new lsa_String();
            }
            base.home_directory.length = (short)_src.dec_ndr_short();
            base.home_directory.size = (short)_src.dec_ndr_short();
            int _base_home_directory_stringp = _src.dec_ndr_long();
            _src.align(4);
            if (base.home_drive == null) {
                base.home_drive = new lsa_String();
            }
            base.home_drive.length = (short)_src.dec_ndr_short();
            base.home_drive.size = (short)_src.dec_ndr_short();
            int _base_home_drive_stringp = _src.dec_ndr_long();
            base.logon_count = (short)_src.dec_ndr_short();
            base.bad_password_count = (short)_src.dec_ndr_short();
            base.rid = (int)_src.dec_ndr_long();
            base.primary_gid = (int)_src.dec_ndr_long();
            _src.align(4);
            if (base.groups == null) {
                base.groups = new samr_RidWithAttributeArray();
            }
            base.groups.count = (int)_src.dec_ndr_long();
            int _base_groups_ridsp = _src.dec_ndr_long();
            base.user_flags = (int)_src.dec_ndr_long();
            _src.align(1);
            if (base.key == null) {
                base.key = new netr_UserSessionKey();
            }
            int _base_key_keys = 16;
            int _base_key_keyi = _src.index;
            _src.advance(1 * _base_key_keys);
            _src.align(4);
            if (base.logon_server == null) {
                base.logon_server = new lsa_StringLarge();
            }
            base.logon_server.length = (short)_src.dec_ndr_short();
            base.logon_server.size = (short)_src.dec_ndr_short();
            int _base_logon_server_stringp = _src.dec_ndr_long();
            _src.align(4);
            if (base.domain == null) {
                base.domain = new lsa_StringLarge();
            }
            base.domain.length = (short)_src.dec_ndr_short();
            base.domain.size = (short)_src.dec_ndr_short();
            int _base_domain_stringp = _src.dec_ndr_long();
            int _base_domain_sidp = _src.dec_ndr_long();
            _src.align(1);
            if (base.LMSessKey == null) {
                base.LMSessKey = new netr_LMSessionKey();
            }
            int _base_LMSessKey_keys = 8;
            int _base_LMSessKey_keyi = _src.index;
            _src.advance(1 * _base_LMSessKey_keys);
            base.acct_flags = (int)_src.dec_ndr_long();
            int _base_unknowns = 7;
            int _base_unknowni = _src.index;
            _src.advance(4 * _base_unknowns);
            sidcount = (int)_src.dec_ndr_long();
            int _sidsp = _src.dec_ndr_long();
            _src.align(4);
            if (forest == null) {
                forest = new lsa_String();
            }
            forest.length = (short)_src.dec_ndr_short();
            forest.size = (short)_src.dec_ndr_short();
            int _forest_stringp = _src.dec_ndr_long();
            _src.align(4);
            if (principle == null) {
                principle = new lsa_String();
            }
            principle.length = (short)_src.dec_ndr_short();
            principle.size = (short)_src.dec_ndr_short();
            int _principle_stringp = _src.dec_ndr_long();
            int _unknown4s = 20;
            int _unknown4i = _src.index;
            _src.advance(4 * _unknown4s);

            if (_base_account_name_stringp != 0) {
                _src = _src.deferred;
                int _base_account_name_strings = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _base_account_name_stringl = _src.dec_ndr_long();
                int _base_account_name_stringi = _src.index;
                _src.advance(2 * _base_account_name_stringl);

                if (base.account_name.string == null) {
                    if (_base_account_name_strings < 0 || _base_account_name_strings > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    base.account_name.string = new short[_base_account_name_strings];
                }
                _src = _src.derive(_base_account_name_stringi);
                for (int _i = 0; _i < _base_account_name_stringl; _i++) {
                    base.account_name.string[_i] = (short)_src.dec_ndr_short();
                }
            }
            if (_base_full_name_stringp != 0) {
                _src = _src.deferred;
                int _base_full_name_strings = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _base_full_name_stringl = _src.dec_ndr_long();
                int _base_full_name_stringi = _src.index;
                _src.advance(2 * _base_full_name_stringl);

                if (base.full_name.string == null) {
                    if (_base_full_name_strings < 0 || _base_full_name_strings > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    base.full_name.string = new short[_base_full_name_strings];
                }
                _src = _src.derive(_base_full_name_stringi);
                for (int _i = 0; _i < _base_full_name_stringl; _i++) {
                    base.full_name.string[_i] = (short)_src.dec_ndr_short();
                }
            }
            if (_base_logon_script_stringp != 0) {
                _src = _src.deferred;
                int _base_logon_script_strings = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _base_logon_script_stringl = _src.dec_ndr_long();
                int _base_logon_script_stringi = _src.index;
                _src.advance(2 * _base_logon_script_stringl);

                if (base.logon_script.string == null) {
                    if (_base_logon_script_strings < 0 || _base_logon_script_strings > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    base.logon_script.string = new short[_base_logon_script_strings];
                }
                _src = _src.derive(_base_logon_script_stringi);
                for (int _i = 0; _i < _base_logon_script_stringl; _i++) {
                    base.logon_script.string[_i] = (short)_src.dec_ndr_short();
                }
            }
            if (_base_profile_path_stringp != 0) {
                _src = _src.deferred;
                int _base_profile_path_strings = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _base_profile_path_stringl = _src.dec_ndr_long();
                int _base_profile_path_stringi = _src.index;
                _src.advance(2 * _base_profile_path_stringl);

                if (base.profile_path.string == null) {
                    if (_base_profile_path_strings < 0 || _base_profile_path_strings > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    base.profile_path.string = new short[_base_profile_path_strings];
                }
                _src = _src.derive(_base_profile_path_stringi);
                for (int _i = 0; _i < _base_profile_path_stringl; _i++) {
                    base.profile_path.string[_i] = (short)_src.dec_ndr_short();
                }
            }
            if (_base_home_directory_stringp != 0) {
                _src = _src.deferred;
                int _base_home_directory_strings = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _base_home_directory_stringl = _src.dec_ndr_long();
                int _base_home_directory_stringi = _src.index;
                _src.advance(2 * _base_home_directory_stringl);

                if (base.home_directory.string == null) {
                    if (_base_home_directory_strings < 0 || _base_home_directory_strings > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    base.home_directory.string = new short[_base_home_directory_strings];
                }
                _src = _src.derive(_base_home_directory_stringi);
                for (int _i = 0; _i < _base_home_directory_stringl; _i++) {
                    base.home_directory.string[_i] = (short)_src.dec_ndr_short();
                }
            }
            if (_base_home_drive_stringp != 0) {
                _src = _src.deferred;
                int _base_home_drive_strings = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _base_home_drive_stringl = _src.dec_ndr_long();
                int _base_home_drive_stringi = _src.index;
                _src.advance(2 * _base_home_drive_stringl);

                if (base.home_drive.string == null) {
                    if (_base_home_drive_strings < 0 || _base_home_drive_strings > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    base.home_drive.string = new short[_base_home_drive_strings];
                }
                _src = _src.derive(_base_home_drive_stringi);
                for (int _i = 0; _i < _base_home_drive_stringl; _i++) {
                    base.home_drive.string[_i] = (short)_src.dec_ndr_short();
                }
            }
            if (_base_groups_ridsp != 0) {
                _src = _src.deferred;
                int _base_groups_ridss = _src.dec_ndr_long();
                int _base_groups_ridsi = _src.index;
                _src.advance(8 * _base_groups_ridss);

                if (base.groups.rids == null) {
                    if (_base_groups_ridss < 0 || _base_groups_ridss > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    base.groups.rids = new samr_RidWithAttribute[_base_groups_ridss];
                }
                _src = _src.derive(_base_groups_ridsi);
                for (int _i = 0; _i < _base_groups_ridss; _i++) {
                    if (base.groups.rids[_i] == null) {
                        base.groups.rids[_i] = new samr_RidWithAttribute();
                    }
                    base.groups.rids[_i].decode(_src);
                }
            }
            if (base.key.key == null) {
                if (_base_key_keys < 0 || _base_key_keys > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                base.key.key = new byte[_base_key_keys];
            }
            _src = _src.derive(_base_key_keyi);
            for (int _i = 0; _i < _base_key_keys; _i++) {
                base.key.key[_i] = (byte)_src.dec_ndr_small();
            }
            if (_base_logon_server_stringp != 0) {
                _src = _src.deferred;
                int _base_logon_server_strings = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _base_logon_server_stringl = _src.dec_ndr_long();
                int _base_logon_server_stringi = _src.index;
                _src.advance(2 * _base_logon_server_stringl);

                if (base.logon_server.string == null) {
                    if (_base_logon_server_strings < 0 || _base_logon_server_strings > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    base.logon_server.string = new short[_base_logon_server_strings];
                }
                _src = _src.derive(_base_logon_server_stringi);
                for (int _i = 0; _i < _base_logon_server_stringl; _i++) {
                    base.logon_server.string[_i] = (short)_src.dec_ndr_short();
                }
            }
            if (_base_domain_stringp != 0) {
                _src = _src.deferred;
                int _base_domain_strings = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _base_domain_stringl = _src.dec_ndr_long();
                int _base_domain_stringi = _src.index;
                _src.advance(2 * _base_domain_stringl);

                if (base.domain.string == null) {
                    if (_base_domain_strings < 0 || _base_domain_strings > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    base.domain.string = new short[_base_domain_strings];
                }
                _src = _src.derive(_base_domain_stringi);
                for (int _i = 0; _i < _base_domain_stringl; _i++) {
                    base.domain.string[_i] = (short)_src.dec_ndr_short();
                }
            }
            if (_base_domain_sidp != 0) {
                if (base.domain_sid == null) { /* YOYOYO */
                    base.domain_sid = new dom_sid();
                }
                _src = _src.deferred;
                base.domain_sid.decode(_src);

            }
            if (base.LMSessKey.key == null) {
                if (_base_LMSessKey_keys < 0 || _base_LMSessKey_keys > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                base.LMSessKey.key = new byte[_base_LMSessKey_keys];
            }
            _src = _src.derive(_base_LMSessKey_keyi);
            for (int _i = 0; _i < _base_LMSessKey_keys; _i++) {
                base.LMSessKey.key[_i] = (byte)_src.dec_ndr_small();
            }
            if (base.unknown == null) {
                if (_base_unknowns < 0 || _base_unknowns > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                base.unknown = new int[_base_unknowns];
            }
            _src = _src.derive(_base_unknowni);
            for (int _i = 0; _i < _base_unknowns; _i++) {
                base.unknown[_i] = (int)_src.dec_ndr_long();
            }
            if (_sidsp != 0) {
                _src = _src.deferred;
                int _sidss = _src.dec_ndr_long();
                int _sidsi = _src.index;
                _src.advance(8 * _sidss);

                if (sids == null) {
                    if (_sidss < 0 || _sidss > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    sids = new netr_SidAttr[_sidss];
                }
                _src = _src.derive(_sidsi);
                for (int _i = 0; _i < _sidss; _i++) {
                    if (sids[_i] == null) {
                        sids[_i] = new netr_SidAttr();
                    }
                    sids[_i].decode(_src);
                }
            }
            if (_forest_stringp != 0) {
                _src = _src.deferred;
                int _forest_strings = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _forest_stringl = _src.dec_ndr_long();
                int _forest_stringi = _src.index;
                _src.advance(2 * _forest_stringl);

                if (forest.string == null) {
                    if (_forest_strings < 0 || _forest_strings > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    forest.string = new short[_forest_strings];
                }
                _src = _src.derive(_forest_stringi);
                for (int _i = 0; _i < _forest_stringl; _i++) {
                    forest.string[_i] = (short)_src.dec_ndr_short();
                }
            }
            if (_principle_stringp != 0) {
                _src = _src.deferred;
                int _principle_strings = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _principle_stringl = _src.dec_ndr_long();
                int _principle_stringi = _src.index;
                _src.advance(2 * _principle_stringl);

                if (principle.string == null) {
                    if (_principle_strings < 0 || _principle_strings > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    principle.string = new short[_principle_strings];
                }
                _src = _src.derive(_principle_stringi);
                for (int _i = 0; _i < _principle_stringl; _i++) {
                    principle.string[_i] = (short)_src.dec_ndr_short();
                }
            }
            if (unknown4 == null) {
                if (_unknown4s < 0 || _unknown4s > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                unknown4 = new int[_unknown4s];
            }
            _src = _src.derive(_unknown4i);
            for (int _i = 0; _i < _unknown4s; _i++) {
                unknown4[_i] = (int)_src.dec_ndr_long();
            }
        }
    }
    public static class netr_LogonSamLogon extends DcerpcMessage {

        public int getOpnum() { return 2; }

        public int retval;

        public netr_LogonSamLogon() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_LogonSamLogoff extends DcerpcMessage {

        public int getOpnum() { return 3; }

        public int retval;

        public netr_LogonSamLogoff() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_ServerReqChallenge extends DcerpcMessage {

        public int getOpnum() { return 4; }

        public int retval;

        public netr_ServerReqChallenge() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_ServerAuthenticate extends DcerpcMessage {

        public int getOpnum() { return 5; }

        public int retval;

        public netr_ServerAuthenticate() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_ServerPasswordSet extends DcerpcMessage {

        public int getOpnum() { return 6; }

        public int retval;

        public netr_ServerPasswordSet() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_DatabaseDeltas extends DcerpcMessage {

        public int getOpnum() { return 7; }

        public int retval;

        public netr_DatabaseDeltas() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_DatabaseSync extends DcerpcMessage {

        public int getOpnum() { return 8; }

        public int retval;

        public netr_DatabaseSync() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_AccountDeltas extends DcerpcMessage {

        public int getOpnum() { return 9; }

        public int retval;

        public netr_AccountDeltas() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_AccountSync extends DcerpcMessage {

        public int getOpnum() { return 10; }

        public int retval;

        public netr_AccountSync() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_GetDcName extends DcerpcMessage {

        public int getOpnum() { return 11; }

        public int retval;

        public netr_GetDcName() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_LogonControl extends DcerpcMessage {

        public int getOpnum() { return 12; }

        public int retval;

        public netr_LogonControl() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_GetAnyDCName extends DcerpcMessage {

        public int getOpnum() { return 13; }

        public int retval;

        public netr_GetAnyDCName() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_LogonControl2 extends DcerpcMessage {

        public int getOpnum() { return 14; }

        public int retval;

        public netr_LogonControl2() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_ServerAuthenticate2 extends DcerpcMessage {

        public int getOpnum() { return 15; }

        public int retval;

        public netr_ServerAuthenticate2() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_DatabaseSync2 extends DcerpcMessage {

        public int getOpnum() { return 16; }

        public int retval;

        public netr_DatabaseSync2() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_DatabaseRedo extends DcerpcMessage {

        public int getOpnum() { return 17; }

        public int retval;

        public netr_DatabaseRedo() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_LogonControl2Ex extends DcerpcMessage {

        public int getOpnum() { return 18; }

        public int retval;

        public netr_LogonControl2Ex() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_NetrEnumerateTrustedDomains extends DcerpcMessage {

        public int getOpnum() { return 19; }

        public int retval;

        public netr_NetrEnumerateTrustedDomains() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_DsRGetDCName extends DcerpcMessage {

        public int getOpnum() { return 20; }

        public int retval;

        public netr_DsRGetDCName() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_NETRLOGONDUMMYROUTINE1 extends DcerpcMessage {

        public int getOpnum() { return 21; }

        public int retval;

        public netr_NETRLOGONDUMMYROUTINE1() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_NETRLOGONSETSERVICEBITS extends DcerpcMessage {

        public int getOpnum() { return 22; }

        public int retval;

        public netr_NETRLOGONSETSERVICEBITS() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_LogonGetTrustRid extends DcerpcMessage {

        public int getOpnum() { return 23; }

        public int retval;

        public netr_LogonGetTrustRid() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_NETRLOGONCOMPUTESERVERDIGEST extends DcerpcMessage {

        public int getOpnum() { return 24; }

        public int retval;

        public netr_NETRLOGONCOMPUTESERVERDIGEST() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_NETRLOGONCOMPUTECLIENTDIGEST extends DcerpcMessage {

        public int getOpnum() { return 25; }

        public int retval;

        public netr_NETRLOGONCOMPUTECLIENTDIGEST() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_ServerAuthenticate3 extends DcerpcMessage {

        public int getOpnum() { return 26; }

        public int retval;

        public netr_ServerAuthenticate3() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_DsRGetDCNameEx extends DcerpcMessage {

        public int getOpnum() { return 27; }

        public int retval;

        public netr_DsRGetDCNameEx() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_DsRGetSiteName extends DcerpcMessage {

        public int getOpnum() { return 28; }

        public int retval;

        public netr_DsRGetSiteName() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_LogonGetDomainInfo extends DcerpcMessage {

        public int getOpnum() { return 29; }

        public int retval;

        public netr_LogonGetDomainInfo() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_ServerPasswordSet2 extends DcerpcMessage {

        public int getOpnum() { return 30; }

        public int retval;

        public netr_ServerPasswordSet2() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_ServerPasswordGet extends DcerpcMessage {

        public int getOpnum() { return 31; }

        public int retval;

        public netr_ServerPasswordGet() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_NETRLOGONSENDTOSAM extends DcerpcMessage {

        public int getOpnum() { return 32; }

        public int retval;

        public netr_NETRLOGONSENDTOSAM() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_DsRAddressToSitenamesW extends DcerpcMessage {

        public int getOpnum() { return 33; }

        public int retval;

        public netr_DsRAddressToSitenamesW() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_DsRGetDCNameEx2 extends DcerpcMessage {

        public int getOpnum() { return 34; }

        public int retval;

        public netr_DsRGetDCNameEx2() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_NETRLOGONGETTIMESERVICEPARENTDOMAIN extends DcerpcMessage {

        public int getOpnum() { return 35; }

        public int retval;

        public netr_NETRLOGONGETTIMESERVICEPARENTDOMAIN() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_NetrEnumerateTrustedDomainsEx extends DcerpcMessage {

        public int getOpnum() { return 36; }

        public int retval;

        public netr_NetrEnumerateTrustedDomainsEx() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_DsRAddressToSitenamesExW extends DcerpcMessage {

        public int getOpnum() { return 37; }

        public int retval;

        public netr_DsRAddressToSitenamesExW() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_DsrGetDcSiteCoverageW extends DcerpcMessage {

        public int getOpnum() { return 38; }

        public int retval;

        public netr_DsrGetDcSiteCoverageW() {
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class netr_LogonSamLogonEx extends DcerpcMessage {

        public int getOpnum() { return 39; }

        public int retval;
        public String server_name;
        public String computer_name;
        public short logon_level;
        public NdrObject logon;
        public short validation_level;
        public NdrObject validation;
        public byte authoritative;
        public int flags;

        public netr_LogonSamLogonEx(String server_name,
                    String computer_name,
                    short logon_level,
                    NdrObject logon,
                    short validation_level,
                    NdrObject validation,
                    byte authoritative,
                    int flags) {
            this.server_name = server_name;
            this.computer_name = computer_name;
            this.logon_level = logon_level;
            this.logon = logon;
            this.validation_level = validation_level;
            this.validation = validation;
            this.authoritative = authoritative;
            this.flags = flags;
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
            _dst.enc_ndr_referent(server_name, 1);
            if (server_name != null) {
                _dst.enc_ndr_string(server_name);

            }
            _dst.enc_ndr_referent(computer_name, 1);
            if (computer_name != null) {
                _dst.enc_ndr_string(computer_name);

            }
            _dst.enc_ndr_short(logon_level);
            short _descr = logon_level;
            _dst.enc_ndr_short(_descr);
            _dst.enc_ndr_referent(logon, 1);
            if (logon != null) {
                _dst = _dst.deferred;
                logon.encode(_dst);

            }
            _dst.enc_ndr_short(validation_level);
            _dst.enc_ndr_long(flags);
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            _src.dec_ndr_short(); /* union discriminant */
            int _validationp = _src.dec_ndr_long();
            if (_validationp != 0) {
                if (validation == null) { /* YOYOYO */
                    validation = new netr_SamInfo2();
                }
                _src = _src.deferred;
                validation.decode(_src);

            }
            authoritative = (byte)_src.dec_ndr_small();
            flags = (int)_src.dec_ndr_long();
            retval = (int)_src.dec_ndr_long();
        }
    }
}
