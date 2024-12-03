package helloswing;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import java.awt.event.ActionListener;

public class DatabaseManager extends javax.swing.JFrame {
    private Connection connection;
    private boolean isLogin;
    private String currentDatabase = null;
    private ActionListener tableComboListener;
    private TreeSelectionListener treeSelectionListener;

    public DatabaseManager(boolean isLogin, Connection connection) {
        this.isLogin = isLogin;
        this.connection = connection;
        if (!this.isLogin) {
            new SQLLogin().setVisible(true);
            this.dispose();
        } else {
            initComponents();
            loadDatabases();
            loadDatabaseTree();
            setLocationRelativeTo(null);
            addTreeSelectionListener();
            txtpnStatus.setText("Connected to MySQL server successfully!\n");
        }
    }

    public void loadDatabases() {        
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SHOW DATABASES");
            comboboxDB.removeAllItems();
            Set<String> systemDatabases = new HashSet<>(Arrays.asList("information_schema", "mysql", "performance_schema", "sys"));
            StringBuilder dbList = new StringBuilder();
            while (rs.next()) {
                String dbName = rs.getString(1);
                if (!systemDatabases.contains(dbName)) {
                    comboboxDB.addItem(dbName);
                    dbList.append(dbName).append("\n");
                }
            }
            this.toFront();
            pack();
        } catch (SQLException e) {
            // e.printStackTrace();
            this.toFront();
            JOptionPane.showMessageDialog(this, "Failed to load databases");
        }
    }

    private void loadTables(String database) {
        if (tableComboListener != null) {
            comboboxTableDB.removeActionListener(tableComboListener);
            tableComboListener = null;
        }
        comboboxTableDB.removeAllItems();
        if (database == null || database.trim().isEmpty()) {
            return;
        }
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema = '" + database + "'");
            while (rs.next()) {
                String tableName = rs.getString("table_name");
                comboboxTableDB.addItem(tableName);
            }
        } catch (SQLException e) {
            // e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to load tables");
        }
    }

    private void loadTableData(String tableName) {
        if (currentDatabase == null) {
            txtpnStatus.setText(txtpnStatus.getText() + "Please select a database first\n");
            return;
        }
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM " + currentDatabase + "." + tableName);
            java.sql.ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            String[] columnNames = new String[columnCount];
            for (int i = 1; i <= columnCount; i++) {
                columnNames[i - 1] = metaData.getColumnName(i);
            }
            javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(columnNames, 0);
            while (rs.next()) {
                Object[] row = new Object[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    row[i - 1] = rs.getObject(i);
                }
                model.addRow(row);
            }
            tblDB.setModel(model);
            tblDB.setFont(new java.awt.Font("Segoe UI", 0, 18));
            tblDB.setRowHeight(30);
            tblDB.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
            txtpnStatus.setText(txtpnStatus.getText() + "Successfully loaded " + tableName + " of " + currentDatabase + "!\n");
        } catch (SQLException e) {
            txtpnStatus.setText(txtpnStatus.getText() + "Error loading table: " + tableName + "\nError: " + e.getMessage() + "\n");
            JOptionPane.showMessageDialog(this, "Failed to load table " + tableName + " of " + currentDatabase, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadDatabaseTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Databases");
        try (Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SHOW DATABASES")) {    
            Set<String> systemDatabases = new HashSet<>(Arrays.asList("information_schema", "mysql", "performance_schema", "sys"));
            while (rs.next()) {
                String dbName = rs.getString(1);
                if (!systemDatabases.contains(dbName)) {
                    DefaultMutableTreeNode dbNode = new DefaultMutableTreeNode(dbName);
                    root.add(dbNode);    
                    try (Statement tableStmt = connection.createStatement();
                        ResultSet tableRs = tableStmt.executeQuery("SHOW TABLES FROM " + dbName)) {    
                        while (tableRs.next()) {
                            String tableName = tableRs.getString(1);
                            dbNode.add(new DefaultMutableTreeNode(tableName));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load database tree: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        DBTree.setModel(new DefaultTreeModel(root));
    }

    private void handleDatabaseSelection(String database) {
        try {
            if (database != null && !database.isEmpty()) {
                Statement stmt = connection.createStatement();
                stmt.execute("USE " + database);
                currentDatabase = database;
                loadTables(database);
                txtpnStatus.setText(txtpnStatus.getText() + "Database selected: " + database + "\n");
            }
        } catch (SQLException e) {
            txtpnStatus.setText(txtpnStatus.getText() + "Error selecting database: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Failed to select database", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addTreeSelectionListener() {
        treeSelectionListener = e -> {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) DBTree.getLastSelectedPathComponent();
            if (selectedNode == null) return;    
            if (selectedNode.getLevel() == 1) {
                String selectedDatabase = selectedNode.getUserObject().toString();
                handleDatabaseSelection(selectedDatabase);
            } else if (selectedNode.getLevel() == 2) {
                DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) selectedNode.getParent();
                String selectedDatabase = parentNode.getUserObject().toString();
                handleDatabaseSelection(selectedDatabase); // Ensure the parent database is selected first
                String selectedTable = selectedNode.getUserObject().toString();
                loadTableData(selectedTable);
            }
        };
        DBTree.addTreeSelectionListener(treeSelectionListener);
    }

    private void removeTreeSelectionListener() {
        if (treeSelectionListener != null) {
            DBTree.removeTreeSelectionListener(treeSelectionListener);
            treeSelectionListener = null;
        }
    }
    
    public void refreshComboBoxes() {
        loadDatabases();
        String selectedDatabase = (String) comboboxDB.getSelectedItem();
        if (selectedDatabase != null) {
            loadTables(selectedDatabase);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        pnMain = new javax.swing.JPanel();
        pnFile = new javax.swing.JPanel();
        btnShow = new javax.swing.JButton();
        btnNew = new javax.swing.JButton();
        btnClose = new javax.swing.JButton();
        pnCommand = new javax.swing.JPanel();
        btnSelect = new javax.swing.JButton();
        btnInsert = new javax.swing.JButton();
        btnUpdate = new javax.swing.JButton();
        btnCreate = new javax.swing.JButton();
        btnDelete = new javax.swing.JButton();
        btnDrop = new javax.swing.JButton();
        pnStatus = new javax.swing.JPanel();
        btnClear = new javax.swing.JButton();
        sclpnStatus = new javax.swing.JScrollPane();
        txtpnStatus = new javax.swing.JTextPane();
        pnCurrentDB = new javax.swing.JPanel();
        comboboxDB = new javax.swing.JComboBox<>();
        lblCurrentDB = new javax.swing.JLabel();
        comboboxTableDB = new javax.swing.JComboBox<>();
        btnChooseDB = new javax.swing.JButton();
        btnRefreshCombobox = new javax.swing.JButton();
        pnTable = new javax.swing.JPanel();
        sclpnTableDB = new javax.swing.JScrollPane();
        tblDB = new javax.swing.JTable();
        pnTree = new javax.swing.JPanel();
        SclpnTree = new javax.swing.JScrollPane();
        DBTree = new javax.swing.JTree();
        btnRefreshTree = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Database Manager");
        setResizable(false);

        pnMain.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        pnMain.setPreferredSize(new java.awt.Dimension(1251, 748));

        pnFile.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED), "File"));
        pnFile.setLayout(new java.awt.GridLayout(1, 0));

        btnShow.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        btnShow.setText("SHOW");
        btnShow.setToolTipText("Show database tables");
        btnShow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnShowActionPerformed(evt);
            }
        });
        pnFile.add(btnShow);

        btnNew.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        btnNew.setText("NEW");
        btnNew.setToolTipText("Create new database");
        btnNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNewActionPerformed(evt);
            }
        });
        pnFile.add(btnNew);

        btnClose.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        btnClose.setText("CLOSE");
        btnClose.setToolTipText("Close current database");
        btnClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCloseActionPerformed(evt);
            }
        });
        pnFile.add(btnClose);

        pnCommand.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED), "Commands"));
        pnCommand.setPreferredSize(new java.awt.Dimension(240, 120));
        pnCommand.setLayout(new java.awt.GridLayout(2, 0));

        btnSelect.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        btnSelect.setText("SELECT");
        btnSelect.setToolTipText("Get data from one or more tables");
        btnSelect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSelectActionPerformed(evt);
            }
        });
        pnCommand.add(btnSelect);

        btnInsert.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        btnInsert.setText("INSERT");
        btnInsert.setToolTipText("Insert new data into a table");
        btnInsert.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnInsertActionPerformed(evt);
            }
        });
        pnCommand.add(btnInsert);

        btnUpdate.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        btnUpdate.setText("UPDATE");
        btnUpdate.setToolTipText("Update existing data in the table");
        btnUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUpdateActionPerformed(evt);
            }
        });
        pnCommand.add(btnUpdate);

        btnCreate.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        btnCreate.setText("CREATE");
        btnCreate.setToolTipText("Create a new table");
        btnCreate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCreateActionPerformed(evt);
            }
        });
        pnCommand.add(btnCreate);

        btnDelete.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        btnDelete.setText("DELETE");
        btnDelete.setToolTipText("Delete data from table");
        btnDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteActionPerformed(evt);
            }
        });
        pnCommand.add(btnDelete);

        btnDrop.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        btnDrop.setText("DROP");
        btnDrop.setToolTipText("Completely delete a table from the database");
        btnDrop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDropActionPerformed(evt);
            }
        });
        pnCommand.add(btnDrop);

        pnStatus.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED), "Status"));

        btnClear.setText("Clear");
        btnClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClearActionPerformed(evt);
            }
        });

        txtpnStatus.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        sclpnStatus.setViewportView(txtpnStatus);

        javax.swing.GroupLayout pnStatusLayout = new javax.swing.GroupLayout(pnStatus);
        pnStatus.setLayout(pnStatusLayout);
        pnStatusLayout.setHorizontalGroup(
            pnStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(btnClear, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(sclpnStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 289, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        pnStatusLayout.setVerticalGroup(
            pnStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnStatusLayout.createSequentialGroup()
                .addComponent(sclpnStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 483, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnClear))
        );

        pnCurrentDB.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        pnCurrentDB.setPreferredSize(new java.awt.Dimension(4, 36));

        comboboxDB.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        comboboxDB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboboxDBActionPerformed(evt);
            }
        });

        lblCurrentDB.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        lblCurrentDB.setText("Current Database");

        comboboxTableDB.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        comboboxTableDB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboboxTableDBActionPerformed(evt);
            }
        });

        btnChooseDB.setText("Choose");
        btnChooseDB.setToolTipText("Choose this table of this database");
        btnChooseDB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnChooseDBActionPerformed(evt);
            }
        });

        btnRefreshCombobox.setText("Refresh");
        btnRefreshCombobox.setToolTipText("Refresh combo box");
        btnRefreshCombobox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRefreshComboboxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnCurrentDBLayout = new javax.swing.GroupLayout(pnCurrentDB);
        pnCurrentDB.setLayout(pnCurrentDBLayout);
        pnCurrentDBLayout.setHorizontalGroup(
            pnCurrentDBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnCurrentDBLayout.createSequentialGroup()
                .addComponent(lblCurrentDB, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(comboboxDB, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(comboboxTableDB, javax.swing.GroupLayout.PREFERRED_SIZE, 129, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnChooseDB)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnRefreshCombobox)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        pnCurrentDBLayout.setVerticalGroup(
            pnCurrentDBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnCurrentDBLayout.createSequentialGroup()
                .addComponent(lblCurrentDB)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(pnCurrentDBLayout.createSequentialGroup()
                .addGroup(pnCurrentDBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(comboboxDB)
                    .addComponent(comboboxTableDB)
                    .addComponent(btnChooseDB)
                    .addComponent(btnRefreshCombobox))
                .addContainerGap())
        );

        pnTable.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED), "Table", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));
        pnTable.setLayout(new java.awt.GridBagLayout());

        tblDB.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        tblDB.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        tblDB.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        sclpnTableDB.setViewportView(tblDB);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 904;
        gridBagConstraints.ipady = 650;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(20, 20, 14, 17);
        pnTable.add(sclpnTableDB, gridBagConstraints);

        pnTree.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED), "Database Tree"));

        DBTree.setToolTipText("");
        SclpnTree.setViewportView(DBTree);

        btnRefreshTree.setText("Refresh");
        btnRefreshTree.setToolTipText("Refresh tree");
        btnRefreshTree.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRefreshTreeActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnTreeLayout = new javax.swing.GroupLayout(pnTree);
        pnTree.setLayout(pnTreeLayout);
        pnTreeLayout.setHorizontalGroup(
            pnTreeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnTreeLayout.createSequentialGroup()
                .addGroup(pnTreeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(SclpnTree, javax.swing.GroupLayout.DEFAULT_SIZE, 138, Short.MAX_VALUE)
                    .addComponent(btnRefreshTree, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        pnTreeLayout.setVerticalGroup(
            pnTreeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnTreeLayout.createSequentialGroup()
                .addComponent(SclpnTree, javax.swing.GroupLayout.PREFERRED_SIZE, 669, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnRefreshTree))
        );

        javax.swing.GroupLayout pnMainLayout = new javax.swing.GroupLayout(pnMain);
        pnMain.setLayout(pnMainLayout);
        pnMainLayout.setHorizontalGroup(
            pnMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnMainLayout.createSequentialGroup()
                .addGroup(pnMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(pnFile, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pnCommand, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pnStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnTree, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnTable, javax.swing.GroupLayout.PREFERRED_SIZE, 819, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
            .addComponent(pnCurrentDB, javax.swing.GroupLayout.DEFAULT_SIZE, 1288, Short.MAX_VALUE)
        );
        pnMainLayout.setVerticalGroup(
            pnMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnMainLayout.createSequentialGroup()
                .addGroup(pnMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnMainLayout.createSequentialGroup()
                        .addComponent(pnFile, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(pnCommand, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(pnStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(pnTable, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(pnMainLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(pnTree, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnCurrentDB, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pnMain, javax.swing.GroupLayout.DEFAULT_SIZE, 1292, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pnMain, javax.swing.GroupLayout.DEFAULT_SIZE, 772, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnRefreshTreeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefreshTreeActionPerformed
        loadDatabases();       
        addTreeSelectionListener(); 
        loadDatabaseTree();
        removeTreeSelectionListener();
    }//GEN-LAST:event_btnRefreshTreeActionPerformed

    private void btnRefreshComboboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefreshComboboxActionPerformed
        refreshComboBoxes();
    }//GEN-LAST:event_btnRefreshComboboxActionPerformed

    private void btnNewActionPerformed(java.awt.event.ActionEvent evt) {
        String dbName = JOptionPane.showInputDialog(this, "Enter new database name:");
        if (dbName != null && !dbName.trim().isEmpty()) {
            try {
                if (!dbName.matches("^[a-zA-Z0-9_]+$")) {
                    JOptionPane.showMessageDialog(this, "Database name can only contain letters, numbers and underscore", "Invalid Name", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                Statement stmt = connection.createStatement();
                stmt.executeUpdate("CREATE DATABASE `" + dbName + "`");
                loadDatabases();
                StyledDocument doc = txtpnStatus.getStyledDocument();
                doc.insertString(doc.getLength(), "Created new database: " + dbName + "\n", null);
                JOptionPane.showMessageDialog(this, "Database '" + dbName + "' created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException | BadLocationException e) {
                JOptionPane.showMessageDialog(this, "Error creating database: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void btnUpdateActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnUpdateActionPerformed
        if (currentDatabase == null) {
            JOptionPane.showMessageDialog(this, "Please select a database first", "No Database", JOptionPane.WARNING_MESSAGE);
            return;
        }    
        String selectedTable = (String) comboboxTableDB.getSelectedItem();
        if (selectedTable == null) {
            JOptionPane.showMessageDialog(this, "Please select a table first", "No Table", JOptionPane.WARNING_MESSAGE);
            return;
        }    
        int selectedRow = tblDB.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a row to update", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }    
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("DESCRIBE " + currentDatabase + "." + selectedTable);    
            StringBuilder setClause = new StringBuilder();
            StringBuilder whereClause = new StringBuilder();
            boolean firstSet = true;
            boolean firstWhere = true;    
            while (rs.next()) {
                String columnName = rs.getString("Field");
                String columnType = rs.getString("Type");
                int columnIndex = tblDB.getColumnModel().getColumnIndex(columnName);
                String oldValue = tblDB.getValueAt(selectedRow, columnIndex).toString();    
                String newValue = JOptionPane.showInputDialog(this,
                    "Update " + columnName + " (" + columnType + ")\nCurrent value: " + oldValue,
                    oldValue);    
                if (newValue != null && !newValue.equals(oldValue)) {
                    if (!firstSet) {
                        setClause.append(", ");
                    }
                    setClause.append(columnName).append("=");
                    if (columnType.contains("char") || columnType.contains("text") || columnType.contains("date")) {
                        setClause.append("'").append(newValue).append("'");
                    } else {
                        setClause.append(newValue);
                    }
                    firstSet = false;
                }
                if (!firstWhere) {
                    whereClause.append(" AND ");
                }
                whereClause.append(columnName).append("=");
                if (columnType.contains("char") || columnType.contains("text") || columnType.contains("date")) {
                    whereClause.append("'").append(oldValue).append("'");
                } else {
                    whereClause.append(oldValue);
                }
                firstWhere = false;
            }    
            if (setClause.length() > 0) {
                String query = "UPDATE " + selectedTable + " SET " + setClause + " WHERE " + whereClause;
                stmt.executeUpdate(query);
                loadTableData(selectedTable);
                txtpnStatus.setText(txtpnStatus.getText() + "Updated record in " + selectedTable + "\n");
                JOptionPane.showMessageDialog(this, "Record updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                txtpnStatus.setText(txtpnStatus.getText() + "No changes made to record\n");
            }    
        } catch (SQLException e) {
            txtpnStatus.setText(txtpnStatus.getText() + "Error updating record: " + e.getMessage() + "\n");
            JOptionPane.showMessageDialog(this, "Error updating record: " + e.getMessage(), "Update Error", JOptionPane.ERROR_MESSAGE);
        }
    }// GEN-LAST:event_btnUpdateActionPerformed

    private void btnShowActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnShowActionPerformed
        String selectedDB = (String) comboboxDB.getSelectedItem();
        if (selectedDB != null) {
            try {
                Statement stmt = connection.createStatement();
                stmt.execute("USE " + selectedDB);
                StyledDocument doc = txtpnStatus.getStyledDocument();
                try {
                    doc.insertString(doc.getLength(), "Show tables of " + selectedDB + "\n", null);
                } catch (BadLocationException e) {
                    // e.printStackTrace();
                }
                ResultSet rs = stmt.executeQuery("SHOW TABLES");
                while (rs.next()) {
                    String tableName = rs.getString(1);
                    doc.insertString(doc.getLength(), "Table: " + tableName + "\n", null);
                }
                btnShow.setEnabled(false);
                btnClose.setEnabled(true);
            } catch (SQLException | BadLocationException e) {
                JOptionPane.showMessageDialog(this, "Error opening database: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }// GEN-LAST:event_btnShowActionPerformed

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnCloseActionPerformed
        try {
            Statement stmt = connection.createStatement();
            stmt.execute("USE mysql");

            // Update status
            StyledDocument doc = txtpnStatus.getStyledDocument();
            doc.insertString(doc.getLength(), "Database closed\n", null);
            btnShow.setEnabled(true);
            btnClose.setEnabled(false);
        } catch (SQLException | BadLocationException e) {
            JOptionPane.showMessageDialog(this, "Error closing database: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }// GEN-LAST:event_btnCloseActionPerformed

    private void btnSelectActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnSelectActionPerformed
        if (currentDatabase == null) {
            JOptionPane.showMessageDialog(this, "Please select a database first", "No Database", JOptionPane.WARNING_MESSAGE);
            return;
        }    
        String query = JOptionPane.showInputDialog(this, 
            "Enter SELECT query:\n(e.g. SELECT * FROM tablename WHERE condition)",
            "SELECT", 
            JOptionPane.PLAIN_MESSAGE);    
        if (query != null && !query.trim().isEmpty()) {
            try {
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(query);
                java.sql.ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                String[] columnNames = new String[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    columnNames[i-1] = metaData.getColumnName(i);
                }
                javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(columnNames, 0);
                while (rs.next()) {
                    Object[] row = new Object[columnCount];
                    for (int i = 1; i <= columnCount; i++) {
                        row[i-1] = rs.getObject(i);
                    }
                    model.addRow(row);
                }
                tblDB.setModel(model);
                txtpnStatus.setText(txtpnStatus.getText() + "Executed query: " + query + "\n");
            } catch (SQLException e) {
                txtpnStatus.setText(txtpnStatus.getText() + "Error executing query: " + e.getMessage() + "\n");
                JOptionPane.showMessageDialog(this, "Error executing query: " + e.getMessage(), "Query Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }// GEN-LAST:event_btnSelectActionPerformed

    private void btnInsertActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnInsertActionPerformed
        if (currentDatabase == null) {
            JOptionPane.showMessageDialog(this, "Please select a database first", "No Database", JOptionPane.WARNING_MESSAGE);
            return;
        }        
        String selectedTable = (String) comboboxTableDB.getSelectedItem();
        if (selectedTable == null) {
            JOptionPane.showMessageDialog(this, "Please select a table first", "No Table", JOptionPane.WARNING_MESSAGE);
            return;
        }    
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("DESCRIBE " + currentDatabase + "." + selectedTable);            
            StringBuilder columns = new StringBuilder();
            StringBuilder values = new StringBuilder();
            boolean first = true;            
            while (rs.next()) {
                String columnName = rs.getString("Field");
                String columnType = rs.getString("Type");                
                String value = JOptionPane.showInputDialog(this,
                    "Enter value for " + columnName + " (" + columnType + "):");                
                if (value != null) {
                    if (!first) {
                        columns.append(", ");
                        values.append(", ");
                    }
                    columns.append(columnName);
                    if (columnType.contains("char") || columnType.contains("text") || columnType.contains("date")) {
                        values.append("'").append(value).append("'");
                    } else {
                        values.append(value);
                    }
                    first = false;
                }
            }    
            String query = "INSERT INTO " + selectedTable + " (" + columns + ") VALUES (" + values + ")";
            stmt.executeUpdate(query);
            loadTableData(selectedTable);            
            txtpnStatus.setText(txtpnStatus.getText() + "Inserted new record into " + selectedTable + "\n");
            JOptionPane.showMessageDialog(this, "Record inserted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);            
        } catch (SQLException e) {
            txtpnStatus.setText(txtpnStatus.getText() + "Error inserting record: " + e.getMessage() + "\n");
            JOptionPane.showMessageDialog(this, "Error inserting record: " + e.getMessage(), "Insert Error", JOptionPane.ERROR_MESSAGE);
        }
    }// GEN-LAST:event_btnInsertActionPerformed

    private void btnCreateActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnCreateActionPerformed
        if (currentDatabase == null) {
            JOptionPane.showMessageDialog(this, "Please select a database first", "No Database", JOptionPane.WARNING_MESSAGE);
            return;
        }    
        String tableName = JOptionPane.showInputDialog(this, "Enter new table name:");
        if (tableName == null || tableName.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Table name cannot be empty", "Invalid Name", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JPanel panel = new JPanel();
        JTextField textField = new JTextField(20);
        textField.setToolTipText("Example: id INT PRIMARY KEY, name VARCHAR(100), ...");
        panel.add(new JLabel("Enter columns:"));
        panel.add(textField);
        int result = JOptionPane.showConfirmDialog(null, panel, "Column Input", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        String columns = textField.getText();
        if (columns == null || columns.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Columns cannot be empty", "Invalid Columns", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            Statement stmt = connection.createStatement();
            String query = "CREATE TABLE " + tableName + " (" + columns + ")";
            stmt.executeUpdate(query);
            loadTables(currentDatabase);    
            txtpnStatus.setText(txtpnStatus.getText() + "Created new table: " + tableName + "\n");
            JOptionPane.showMessageDialog(this, "Table '" + tableName + "' created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            txtpnStatus.setText(txtpnStatus.getText() + "Error creating table: " + e.getMessage() + "\n");
            JOptionPane.showMessageDialog(this, "Error creating table: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }        
    }// GEN-LAST:event_btnCreateActionPerformed

    private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnDeleteActionPerformed
        if (currentDatabase == null) {
            JOptionPane.showMessageDialog(this, "Please select a database first", "No Database", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String selectedTable = (String) comboboxTableDB.getSelectedItem();
        if (selectedTable == null) {
            JOptionPane.showMessageDialog(this, "Please select a table first", "No Table", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int selectedRow = tblDB.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a row to delete", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("DESCRIBE " + currentDatabase + "." + selectedTable);
            StringBuilder whereClause = new StringBuilder();
            boolean firstWhere = true;
            while (rs.next()) {
                String columnName = rs.getString("Field");
                String columnType = rs.getString("Type");
                int columnIndex = tblDB.getColumnModel().getColumnIndex(columnName);
                String value = tblDB.getValueAt(selectedRow, columnIndex).toString();
                if (!firstWhere) {
                    whereClause.append(" AND ");
                }
                whereClause.append(columnName).append("=");
                if (columnType.contains("char") || columnType.contains("text") || columnType.contains("date")) {
                    whereClause.append("'").append(value).append("'");
                } else {
                    whereClause.append(value);
                }
                firstWhere = false;
            }
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this record?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                String query = "DELETE FROM " + selectedTable + " WHERE " + whereClause;
                stmt.executeUpdate(query);
                loadTableData(selectedTable);
                txtpnStatus.setText(txtpnStatus.getText() + "Deleted record from " + selectedTable + "\n");
                JOptionPane.showMessageDialog(this, "Record deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException e) {
            txtpnStatus.setText(txtpnStatus.getText() + "Error deleting record: " + e.getMessage() + "\n");
            JOptionPane.showMessageDialog(this, "Error deleting record: " + e.getMessage(), "Delete Error", JOptionPane.ERROR_MESSAGE);
        }
    }// GEN-LAST:event_btnDeleteActionPerformed

    private void btnDropActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnDropActionPerformed
        if (currentDatabase == null) {
            JOptionPane.showMessageDialog(this, "Please select a database first", "No Database", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String selectedTable = (String) comboboxTableDB.getSelectedItem();
        if (selectedTable == null) {
            JOptionPane.showMessageDialog(this, "Please select a table first", "No Table", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to drop the table: " + selectedTable + "?", "Confirm Drop", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                Statement stmt = connection.createStatement();
                String query = "DROP TABLE " + currentDatabase + "." + selectedTable;
                stmt.executeUpdate(query);
                loadTables(currentDatabase);
                txtpnStatus.setText(txtpnStatus.getText() + "Dropped table: " + selectedTable + "\n");
                JOptionPane.showMessageDialog(this, "Table dropped successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException e) {
                txtpnStatus.setText(txtpnStatus.getText() + "Error dropping table: " + e.getMessage() + "\n");
                JOptionPane.showMessageDialog(this, "Error dropping table: " + e.getMessage(), "Drop Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }// GEN-LAST:event_btnDropActionPerformed

    private void btnClearActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnClearActionPerformed
        txtpnStatus.setText("");
    }// GEN-LAST:event_btnClearActionPerformed

    private void comboboxDBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_comboboxDBActionPerformed        
        String selectedDatabase = (String) comboboxDB.getSelectedItem();
        if (comboboxTableDB.getSelectedItem() != null) {
            comboboxTableDB.setSelectedItem(null);
        }
        handleDatabaseSelection(selectedDatabase);
    }// GEN-LAST:event_comboboxDBActionPerformed

    private void comboboxTableDBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_comboboxTableDBActionPerformed
        if (currentDatabase != null && evt.getSource() == comboboxTableDB) {
            String selectedTable = (String) comboboxTableDB.getSelectedItem();
            if (selectedTable != null) {
                txtpnStatus.setText(txtpnStatus.getText() + "Table selected: " + selectedTable + "\n");
            }
        }
    }// GEN-LAST:event_comboboxTableDBActionPerformed

    private void btnChooseDBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnChooseDBActionPerformed
        if (currentDatabase != null) {
            String selectedTable = (String) comboboxTableDB.getSelectedItem();
            if (selectedTable != null) {
                loadTableData(selectedTable);
            }
        }
    }// GEN-LAST:event_btnChooseDBActionPerformed

    public static void main(String args[]) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(DatabaseManager.class.getName()).log(java.util.logging.Level.SEVERE,
                    null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(DatabaseManager.class.getName()).log(java.util.logging.Level.SEVERE,
                    null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(DatabaseManager.class.getName()).log(java.util.logging.Level.SEVERE,
                    null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(DatabaseManager.class.getName()).log(java.util.logging.Level.SEVERE,
                    null, ex);
        }

        SQLLogin login = new SQLLogin();
        login.setVisible(true);

        if (login.isLoginIn()) {
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    new DatabaseManager(false, null);
                }
            });
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTree DBTree;
    private javax.swing.JScrollPane SclpnTree;
    private javax.swing.JButton btnChooseDB;
    private javax.swing.JButton btnClear;
    private javax.swing.JButton btnClose;
    private javax.swing.JButton btnCreate;
    private javax.swing.JButton btnDelete;
    private javax.swing.JButton btnDrop;
    private javax.swing.JButton btnInsert;
    private javax.swing.JButton btnNew;
    private javax.swing.JButton btnRefreshCombobox;
    private javax.swing.JButton btnRefreshTree;
    private javax.swing.JButton btnSelect;
    private javax.swing.JButton btnShow;
    private javax.swing.JButton btnUpdate;
    private javax.swing.JComboBox<String> comboboxDB;
    private javax.swing.JComboBox<String> comboboxTableDB;
    private javax.swing.JLabel lblCurrentDB;
    private javax.swing.JPanel pnCommand;
    private javax.swing.JPanel pnCurrentDB;
    private javax.swing.JPanel pnFile;
    private javax.swing.JPanel pnMain;
    private javax.swing.JPanel pnStatus;
    private javax.swing.JPanel pnTable;
    private javax.swing.JPanel pnTree;
    private javax.swing.JScrollPane sclpnStatus;
    private javax.swing.JScrollPane sclpnTableDB;
    private javax.swing.JTable tblDB;
    private javax.swing.JTextPane txtpnStatus;
    // End of variables declaration//GEN-END:variables
}