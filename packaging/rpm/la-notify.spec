%global debug_package %{nil}

Name:           la-notify
Version:        %{version}
Release:        1%{?dist}
Summary:        Android notification forwarder for Linux desktop

License:        AGPL-3.0-only
URL:            https://github.com/alessandrolattao/la-notify
Source0:        %{name}-%{version}.tar.gz

%description
HTTP server that receives notifications from Android devices and displays
them on Linux desktop via D-Bus notifications.

%prep
%autosetup

%install
install -Dm755 bin/%{name} %{buildroot}%{_bindir}/%{name}
install -Dm644 packaging/%{name}.service %{buildroot}%{_userunitdir}/%{name}.service
install -Dm644 LICENSE %{buildroot}%{_licensedir}/%{name}/LICENSE

%post
echo ""
echo "To enable LA-notify for your user, run:"
echo "  systemctl --user enable --now la-notify"
echo ""

%files
%license LICENSE
%{_bindir}/%{name}
%{_userunitdir}/%{name}.service

%changelog
* Wed Dec 10 2025 Alessandro Lattao <alessandro@lattao.com>
- Initial package
