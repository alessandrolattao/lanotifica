%global goipath         github.com/alessandrolattao/lanotifica
%global gomodulesmode   mod
%global debug_package   %{nil}
%define _userunitdir    %{_prefix}/lib/systemd/user

Name:           lanotifica
Version:        1.0.1
Release:        1%{?dist}
Summary:        Android notification forwarder for Linux desktop

License:        AGPL-3.0-only
URL:            https://%{goipath}
Source0:        %{url}/archive/v%{version}/%{name}-%{version}.tar.gz

BuildRequires:  golang >= 1.22
BuildRequires:  systemd-rpm-macros

%description
HTTP server that receives notifications from Android devices and displays
them on Linux desktop via D-Bus notifications.

%prep
%autosetup -n %{name}-%{version}

%build
cd server
go build -ldflags "-s -w" -o %{name} ./cmd/lanotifica

%install
install -Dm755 server/%{name} %{buildroot}%{_bindir}/%{name}
install -Dm644 packaging/%{name}.service %{buildroot}%{_userunitdir}/%{name}.service

%post
%systemd_user_post %{name}.service

%preun
%systemd_user_preun %{name}.service

%postun
%systemd_user_postun_with_restart %{name}.service

%files
%license LICENSE
%{_bindir}/%{name}
%{_userunitdir}/%{name}.service

%changelog
* Thu Dec 12 2025 Alessandro Lattao <alessandro@lattao.com>
- Add COPR build support

* Wed Dec 10 2025 Alessandro Lattao <alessandro@lattao.com>
- Initial package
