%global debug_package %{nil}
%define _userunitdir %{_prefix}/lib/systemd/user

Name:           lanotifica
Version:        1.0.2
Release:        1%{?dist}
Summary:        Android notification forwarder for Linux desktop

License:        AGPL-3.0-only
URL:            https://github.com/alessandrolattao/lanotifica
Source0:        %{name}-%{version}.tar.gz

%description
HTTP server that receives notifications from Android devices and displays
them on Linux desktop via D-Bus notifications.

%prep
%autosetup

%install
install -Dm755 bin/%{name} %{buildroot}%{_bindir}/%{name}
install -Dm644 packaging/%{name}.service %{buildroot}%{_userunitdir}/%{name}.service

%post
echo ""
echo "To enable LaNotifica for your user, run:"
echo "  systemctl --user enable --now lanotifica"
echo ""

%files
%license LICENSE
%{_bindir}/%{name}
%{_userunitdir}/%{name}.service

%changelog
* Wed Dec 10 2025 Alessandro Lattao <alessandro@lattao.com>
- Initial package
