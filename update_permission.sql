UPDATE user SET 
    role = 'admin',
    permissions = 'customer:view,customer:add,customer:edit,customer:delete,contact:view,contact:add,contact:edit,contact:delete,interaction:view,interaction:add,interaction:edit,interaction:delete,order:view,order:add,order:edit,order:delete,churn:view,opportunity:view,service:view,product:view,statistics:view,user:view,role:view,log:view'
WHERE username = 'admin2';